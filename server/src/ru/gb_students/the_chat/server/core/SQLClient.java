package ru.gb_students.the_chat.server.core;

import org.sqlite.JDBC;

import java.sql.*;

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

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
