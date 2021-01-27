package ru.gb_students.the_chat.server.core;

import ru.gb_students.the_chat.library.Protocol;
import ru.gb_students.the_chat.network.SocketThread;
import ru.gb_students.the_chat.network.SocketThreadListener;
import java.net.Socket;

public class ClientThread extends SocketThread {

    private String nickname;
    private String login;
    private boolean isAuthorized;
    private boolean isReconnecting;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    public boolean changeNickname(String newNickname) {
        if (SQLClient.changeNickname(nickname, newNickname)) {
            this.nickname = newNickname;
            return true;
        } else {
            sendMessage(Protocol.getChangeNicknameFailed());
            return false;
        }
    }

    public boolean addMessage(String message) {
        if (SQLClient.addMesage(this.getLogin(), message)) {
            return true;
        } else {
            return false;
        }
    }

    public void reconnect() {
        isReconnecting = true;
        close();
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    void authAccept(String login, String nickname) {
        isAuthorized = true;
        this.login = login;
        this.nickname = nickname;
        sendMessage(Protocol.getAuthAccept(nickname));
    }

    void authFail() {
        sendMessage(Protocol.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Protocol.getMsgFormatError(msg));
        close();
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }
}
