package com.pousada.repository;

import com.pousada.config.Database;
import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.MessageStatus;

import java.sql.*;
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
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all messages", e);
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        MessageChannel channel = MessageChannel.valueOf(rs.getString("channel"));
        String to = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");
        MessageStatus status = MessageStatus.valueOf(rs.getString("status"));

        return new Message(id, channel, to, subject, body, status);
    }
}
