package com.pousadamarealta.repository;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages
            ORDER BY id
            """;

        List<Message> messages = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(MessageChannel.valueOf(rs.getString("channel")));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                message.setStatus(MessageStatus.valueOf(rs.getString("status")));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }

        return messages;
    }
}
