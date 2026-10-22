package com.pousadamarealta;

import com.pousadamarealta.models.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static long nextMessageId = 1;

    public static void sendMessage(String channel, String to, String subject, String body) {
        try (Connection conn = Database.getConnection()) {
            String sql = "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, channel);
                stmt.setString(2, to);
                stmt.setString(3, subject);
                stmt.setString(4, body);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT id, channel, to_address, subject, body FROM messages ORDER BY id";
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        messages.add(new Message(
                            rs.getLong("id"),
                            rs.getString("channel"),
                            rs.getString("to_address"),
                            rs.getString("subject"),
                            rs.getString("body")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }
}
