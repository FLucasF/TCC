package com.pousada.service;

import com.pousada.database.Database;
import com.pousada.domain.Message;

import java.sql.*;
import java.util.*;

public class MessageService {
    public static long sendMessage(String channel, String to, String subject, String body) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new RuntimeException("Failed to send message");
    }

    public static List<Message> getAllMessages() throws Exception {
        List<Message> messages = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id")) {

            while (rs.next()) {
                Message msg = new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                messages.add(msg);
            }
        }

        return messages;
    }
}
