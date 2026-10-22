package com.pousadamareal.dao;

import com.pousadamareal.config.DatabaseConfig;
import com.pousadamareal.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    public static void saveMessage(String channel, String recipientEmail, String subject, String body) throws Exception {
        String sql = """
            INSERT INTO messages (channel, recipient_email, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, channel);
            pstmt.setString(2, recipientEmail);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);

            pstmt.executeUpdate();
        }
    }

    public static List<Message> getAllMessages() throws Exception {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient_email, subject, body FROM messages ORDER BY created_at";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("channel"),
                    rs.getString("recipient_email"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }

        return messages;
    }
}
