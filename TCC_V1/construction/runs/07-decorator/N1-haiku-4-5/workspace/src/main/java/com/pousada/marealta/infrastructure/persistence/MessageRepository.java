package com.pousada.marealta.infrastructure.persistence;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageChannel;
import com.pousada.marealta.domain.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) throws SQLException {
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
                    long id = rs.getLong("id");
                    return findById(id);
                }
            }
        }
        throw new SQLException("Failed to insert message");
    }

    public Message findById(long id) throws SQLException {
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Message> findAll() throws SQLException {
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages
            ORDER BY id
            """;

        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }
        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        MessageChannel channel = MessageChannel.fromString(rs.getString("channel"));
        String to = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");
        MessageStatus status = MessageStatus.fromString(rs.getString("status"));

        return new Message(id, channel, to, subject, body, status);
    }
}
