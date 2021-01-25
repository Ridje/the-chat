package ru.gb_students.the_chat.server.core;

import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class SQLClient {

    private static Connection connection;
    private static Statement statement;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(JDBC.PREFIX + "server/clients.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNickname(String login, String password) {
        String query = String.format("select nickname from clients where login='%s' and password='%s'",
                login, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next()) {
                return set.getString("nickname");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static boolean changeNickname(String currentNickname, String newNickname) {
        String query = String.format("UPDATE clients SET nickname = '%s' where nickname = '%s'", newNickname, currentNickname);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static boolean addMesage(String login, String message) {

        String query = String.format("INSERT into messages(client, message) VALUES ('%s', '%s')", login, message);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static ArrayList<HashMap<String, String>> getMessages() {
        String query = String.format("SELECT clients.nickname as nickname, message, timestamp FROM messages LEFT JOIN clients ON messages.client = clients.login ORDER BY timestamp");
        ArrayList<HashMap<String, String>> messages = new ArrayList<>();
        try (ResultSet set = statement.executeQuery(query)) {
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
            connection.close();
        } catch (SQLException throwable) {
            throw new RuntimeException(throwable);
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
