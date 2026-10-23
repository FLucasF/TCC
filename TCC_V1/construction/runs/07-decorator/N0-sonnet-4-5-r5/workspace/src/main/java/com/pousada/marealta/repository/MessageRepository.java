package com.pousada.marealta.repository;

import com.pousada.marealta.config.Database;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;

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

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(rs.getString("channel"));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                message.setStatus(MessageStatus.valueOf(rs.getString("status")));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching messages", e);
        }

        return messages;
    }
}
