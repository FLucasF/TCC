package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long id = rs.getLong(1);
                message.setId(id);
                return message;
            }

            throw new RuntimeException("Failed to get generated ID");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    MessageStatus.valueOf(rs.getString("status"))
                ));
            }

            return messages;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list messages", e);
        }
    }
}
