package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConnection;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public long create(String channel, String to, String subject, String body, MessageStatus status) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "INSERT INTO messages (channel, \"to\", subject, body, status) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, channel);
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.setString(5, status.name());

            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new Exception("Failed to create message");
    }

    public List<Message> getAllMessages() throws Exception {
        List<Message> messages = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body, status FROM messages ORDER BY id");
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    MessageStatus.fromString(rs.getString("status"))
                ));
            }
        }
        return messages;
    }
}
