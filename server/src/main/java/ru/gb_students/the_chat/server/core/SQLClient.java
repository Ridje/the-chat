package ru.gb_students.the_chat.server.core;

import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SQLClient {

    private static Connection connection;
    private static Statement statement;
    private static ArrayList<Statement> statementsList;
    private static PreparedStatement getNickname;
    private static PreparedStatement changeNickname;
    private static PreparedStatement addMessage;
    private static PreparedStatement getMessages;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(JDBC.PREFIX + "server/clients.db");
            initializeStatements();
            addStatementsToCollection();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initializeStatements() throws SQLException {
        statement = connection.createStatement();
        getNickname = connection.prepareStatement("select nickname from clients where login=? and password=?");
        changeNickname = connection.prepareStatement("UPDATE clients SET nickname = ? where nickname = ?");
        addMessage = connection.prepareStatement("INSERT into messages(client, message) VALUES (?, ?)");
        getMessages = connection.prepareStatement("SELECT clients.nickname as nickname, message, timestamp FROM messages " +
                "LEFT JOIN clients ON messages.client = clients.login ORDER BY timestamp\"");
    }

    private static void addStatementsToCollection() {
        statementsList.add(addMessage);
        statementsList.add(getNickname);
        statementsList.add(changeNickname);
        statementsList.add(getMessages);
        statementsList.add(statement);
    }


    public static String getNickname(String login, String password) {
        try {
            getNickname.setString(1, login);
            getNickname.setString(2, password);
            try (ResultSet set = getNickname.executeQuery()) {
                if (set.next()) {
                    return set.getString("nickname");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static boolean changeNickname(String currentNickname, String newNickname) {
        try {
            changeNickname.setString(1, newNickname);
            changeNickname.setString(2, currentNickname);
            changeNickname.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static boolean addMesage(String login, String message) {

        try {
            addMessage.setString(1, login);
            addMessage.setString(2, message);
            addMessage.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static ArrayList<HashMap<String, String>> getMessages() {

        ArrayList<HashMap<String, String>> messages = new ArrayList<>();
        try (ResultSet set = addMessage.executeQuery()) {
            while (set.next()) {
                HashMap<String, String> message= new HashMap<>();
                message.put("nickname", set.getString("nickname"));
                message.put("message", set.getString("message"));
                message.put("timestamp", set.getString("timestamp"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException();
        }

        return messages;
    }

    public static void disconnect() {
        try {
            closeStatements();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void closeStatements() throws SQLException{
        for (Statement closingStatement:statementsList) {
            closingStatement.close();
        }
    }

    //It's documentation alike-code. Should did it in your sqlite database.
    public static String createTableMessages() {
        return "CREATE TABLE \"messages\" ( \"id\" INTEGER, \"client\" TEXT, \"message\" TEXT, \"timestamp\" INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(\"client\") REFERENCES \"clients\"(\"login\"), PRIMARY KEY(\"id\") )";
    }
    public static String createTableClients() {
        return "CREATE TABLE \"clients\" ( \"login\" TEXT NOT NULL, \"password\" TEXT, \"nickname\" TEXT NOT NULL UNIQUE, PRIMARY KEY(\"login\") )";
    }
}
