package com.pousadamarealta.repository;

import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.enums.MessageChannel;
import com.pousadamarealta.domain.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list messages", e);
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            MessageChannel.valueOf(rs.getString("channel")),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
