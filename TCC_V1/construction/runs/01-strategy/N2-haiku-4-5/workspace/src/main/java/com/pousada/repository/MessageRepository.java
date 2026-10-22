package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageRepository.class);

    public long save(Message.Channel channel, String to, String subject, String body) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                INSERT INTO messages (channel, to_address, subject, body)
                VALUES (?, ?, ?, ?)
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            );

            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to save message");
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                """
                SELECT id, channel, to_address, subject, body
                FROM messages
                ORDER BY created_at ASC
                """
            );

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            Message.Channel.valueOf(rs.getString("channel")),
            rs.getString("to_address"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
