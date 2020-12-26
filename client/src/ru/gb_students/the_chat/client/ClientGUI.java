package ru.gb_students.the_chat.client;

import ru.gb_students.the_chat.library.Protocol;
import ru.gb_students.the_chat.network.SocketThread;
import ru.gb_students.the_chat.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class ClientGUI extends JFrame implements ActionListener,
        Thread.UncaughtExceptionHandler, SocketThreadListener {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final String WINDOW_TITLE = "Chat client";
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss] ");

    private final JTextArea log = new JTextArea();

    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("ivan");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientGUI();
            }
        });
    }

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(WIDTH, HEIGHT);
        setTitle(WINDOW_TITLE);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        JScrollPane scrollUsers = new JScrollPane(userList);
        scrollUsers.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(btnLogin);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);
        updateElementsVisible(false);

        add(scrollLog, BorderLayout.CENTER);
        add(scrollUsers, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            disconnect();
        } else {
            throw new RuntimeException("Undefined source: " + src);
        }
    }

    private void connect() {
        try {
            Socket s = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", s);
        } catch (IOException e) {
            e.printStackTrace();
            showException(Thread.currentThread(), e);
        }
    }

    private void disconnect() {
        socketThread.close();
    }

    private void sendMessage() {
        String msg = tfMessage.getText();

        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.grabFocus();
        socketThread.sendMessage(Protocol.getUserBroadcast(msg));
    }

    private void updateElementsVisible(boolean isSocketConnected) {
        panelBottom.setVisible(isSocketConnected);
        panelTop.setVisible(!isSocketConnected);
    }

    private void wrtMsgToLogFile(String msg, String username) {
        try (FileWriter out = new FileWriter("log.txt", true)) {
            out.write(username + ": " + msg + "\n");
            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = String.format("Exception in thread \"%s\" %s: %s\n\tat %s",
                    t.getName(), e.getClass().getCanonicalName(), e.getMessage(), ste[0]);
            JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    /**
     * Socket thread listener methods implementation
     * */

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        putLog("Socket stopped");
        updateElementsVisible(false);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog("Socket ready");
        socketThread.sendMessage(Protocol.getAuthRequest(
                tfLogin.getText(), new String(tfPassword.getPassword())));
        updateElementsVisible(true);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
        updateElementsVisible(false);
    }

    private void handleMessage(String msg) {
        String[] arr = msg.split(Protocol.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Protocol.AUTH_ACCEPT:
                setTitle(WINDOW_TITLE + " nickname: " + arr[1]);
                panelBottom.setVisible(true);
                panelTop.setVisible(false);
                break;
            case Protocol.AUTH_DENIED:
                putLog("Authorization failed");
                break;
            case Protocol.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Protocol.TYPE_BROADCAST:
                putLog(String.format("%s%s: %s",
                        DATE_FORMAT.format(Long.parseLong(arr[1])),
                        arr[2], arr[3]));
                break;
            case Protocol.USER_LIST:
                String users = msg.substring(Protocol.USER_LIST.length() +
                        Protocol.DELIMITER.length());
                String[] usersArr = users.split(Protocol.DELIMITER);
                Arrays.sort(usersArr);
                userList.setListData(usersArr);
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);

        }
    }

}
