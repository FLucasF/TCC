package com.pousada.db;

import com.pousada.models.Message;

import java.sql.*;
import java.util.*;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message create(String channel, String to, String subject, String body) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Message(id, channel, to, subject, body);
                }
            }
        }

        throw new SQLException("Failed to create message");
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapToMessage(rs));
            }
        }

        return messages;
    }

    private Message mapToMessage(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String channel = rs.getString("channel");
        String to = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");

        return new Message(id, channel, to, subject, body);
    }
}
