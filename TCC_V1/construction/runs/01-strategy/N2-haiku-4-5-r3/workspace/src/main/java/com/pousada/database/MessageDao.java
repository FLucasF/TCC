package com.pousada.database;

import com.pousada.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    public static void insert(String channel, String recipient, String subject, String body) throws SQLException {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setString(2, recipient);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.executeUpdate();
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Message message = new Message(
                        rs.getInt("id"),
                        rs.getString("channel"),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("body")
                );
                messages.add(message);
            }
        }
        return messages;
    }
}
