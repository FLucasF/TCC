package com.pousadamarealta.repositories;

import com.pousadamarealta.database.DatabaseConfig;
import com.pousadamarealta.models.Message;

import java.sql.*;
import java.util.*;

public class MessageRepository {
    private final DatabaseConfig dbConfig;

    public MessageRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void save(String channel, String to, String subject, String body) throws SQLException {
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)"
            )) {
                stmt.setString(1, channel);
                stmt.setString(2, to);
                stmt.setString(3, subject);
                stmt.setString(4, body);
                stmt.executeUpdate();
            }
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = dbConfig.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id"
                )) {
                    while (rs.next()) {
                        messages.add(new Message(
                            rs.getInt("id"),
                            rs.getString("channel"),
                            rs.getString("to"),
                            rs.getString("subject"),
                            rs.getString("body")
                        ));
                    }
                }
            }
        }
        return messages;
    }
}
