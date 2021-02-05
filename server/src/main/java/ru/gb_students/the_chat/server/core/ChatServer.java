package ru.gb_students.the_chat.server.core;

import ru.gb_students.the_chat.library.Protocol;
import ru.gb_students.the_chat.network.ServerSocketThread;
import ru.gb_students.the_chat.network.ServerSocketThreadListener;
import ru.gb_students.the_chat.network.SocketThread;
import ru.gb_students.the_chat.network.SocketThreadListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {
    private ServerSocketThread server;
    private Vector<SocketThread> clients;
    private ChatServerListener listener;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
    private ExecutorService threadsManager;

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
        clients = new Vector<>();
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            System.out.println("Server already started");
        } else {
            server = new ServerSocketThread(this, "Server", port, 2000);
            threadsManager = Executors.newCachedThreadPool();
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            System.out.println("Server is not running");
        } else {
            server.interrupt();
        }
    }

    private void putLog(String msg) {

        msg = DATE_FORMAT.format(System.currentTimeMillis()) + Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server socket thread listener methods implementation
     * */
    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server socket thread started");
        SQLClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server socket thread stopped");
        threadsManager.shutdownNow();
        SQLClient.disconnect();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
        }

    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Server socket thread accept timeout");
    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        String name = "SocketThread" + socket.getInetAddress() + ":" + socket.getPort();
        threadsManager.execute(new ClientThread(this, name, socket));
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket thread listener methods implementation
     * */

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");
    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);
        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAllAuthorizedClients(Protocol.getTypeBroadcast(
                    "Server", "user [" + client.getNickname() + "] disconnected"));
        }
        sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthClientMessage(client, msg);
        } else {
            handleNonAuthClientMessage(client, msg);
        }
    }

    private void handleNonAuthClientMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Protocol.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Protocol.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SQLClient.getNickname(login, password);
        if (nickname == null) {
            putLog("Invalid credentials attempt for login = " + login);
            client.authFail();
            return;
        } else {
            ClientThread oldClient = findClientByNickname(nickname);
            client.authAccept(login, nickname);
            if (oldClient == null) {
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", "user [" + nickname + "] connected"));
            } else {
                oldClient.reconnect();
                clients.remove(oldClient);
            }
        }
        sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
    }

    private void handleAuthClientMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Protocol.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Protocol.USER_BROADCAST:
                sendToAllAuthorizedClients(Protocol.getTypeBroadcast(client.getNickname(), arr[1]));
                client.addMessage(arr[1]);
                break;
            case Protocol.CHANGE_NICKNAME:
                String oldNickname = client.getNickname();
                String newNickname = arr[1];
                if (client.changeNickname(newNickname)) {
                    sendToAllAuthorizedClients(Protocol.getTypeBroadcast("Server", oldNickname + " changed nickname to " + newNickname));
                    sendToAllAuthorizedClients(Protocol.getUserList(getUsers()));
                }
                break;
            case Protocol.REQUEST_MESSAGE_LIST:
                String timestampString = arr[1];

                Timestamp timeBorder = new Timestamp(Long.parseLong(timestampString));
                client.sendMessage(Protocol.getMessageList(getMessages(timeBorder)));
                break;
            default:
                client.msgFormatError(msg);
        }
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread recipient = (ClientThread) clients.get(i);
            if (!recipient.isAuthorized()) continue;
            recipient.sendMessage(msg);
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Protocol.DELIMITER);
        }
        return sb.toString();
    }

    private String getMessages() {
        return getMessages(new Timestamp(0));
    }

    private String getMessages(Timestamp timestamp) {
        StringBuilder sb = new StringBuilder();
        ArrayList<HashMap<String, String>> messages = SQLClient.getMessages(String.valueOf(timestamp.getTime()));
        for (HashMap<String, String> message: messages) {
            sb.append("[" + message.get("timestamp") + "]");
            sb.append(" " + message.get("nickname") + ":");
            sb.append(" " + message.get("message") + "").append(Protocol.DELIMITER);
        }
        return sb.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }
}
