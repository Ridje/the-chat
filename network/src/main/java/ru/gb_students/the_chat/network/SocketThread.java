package ru.gb_students.the_chat.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {

    private final SocketThreadListener listener;
    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.socket = socket;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            listener.onSocketStart(this, socket);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this, socket);
            while (!isInterrupted()) {
                String msg = in.readUTF();
                listener.onReceiveString(this, socket, msg);
            }
        } catch (IOException e) {
            listener.onSocketException(this, e);
            close();
        } finally {
            listener.onSocketStop(this);
        }
    }

    /*
    public synchronized boolean areStreamsOpen() {
        return out != null && in != null;
    }*/

    public synchronized boolean sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
            return true;
        } catch (IOException e) {
            listener.onSocketException(this, e);
            close();
            return false;
        }
    }

    public synchronized void close() {
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
    }
}
