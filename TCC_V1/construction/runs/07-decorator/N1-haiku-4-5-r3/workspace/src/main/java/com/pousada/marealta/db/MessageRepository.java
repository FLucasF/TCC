package com.pousada.marealta.db;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static Message save(String channel, String to, String subject, String body, MessageStatus status) throws Exception {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.setString(5, status.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Message(id, channel, to, subject, body, status);
                }
            }
        }
        return null;
    }

    public static List<Message> findAll() throws Exception {
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";

        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(new Message(
                        rs.getLong("id"),
                        rs.getString("channel"),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        MessageStatus.valueOf(rs.getString("status"))
                ));
            }
        }
        return messages;
    }
}
