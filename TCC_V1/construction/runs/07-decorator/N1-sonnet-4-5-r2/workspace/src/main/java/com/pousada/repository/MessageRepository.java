package com.pousada.repository;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.MessageStatus;
import com.pousada.infrastructure.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

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
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages
            ORDER BY id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message(
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    MessageStatus.valueOf(rs.getString("status"))
                );
                message.setId(rs.getLong("id"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
