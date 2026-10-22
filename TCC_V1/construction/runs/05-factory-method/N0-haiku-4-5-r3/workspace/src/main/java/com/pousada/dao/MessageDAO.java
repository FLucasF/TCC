package com.pousada.dao;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDAO.class);

    public void create(String channel, String to, String subject, String body) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)")) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error creating message", e);
            throw new RuntimeException(e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT id, channel, to_address, subject, body, created_at FROM messages ORDER BY created_at"
            );

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching messages", e);
            throw new RuntimeException(e);
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Message(
            rs.getInt("id"),
            rs.getString("channel"),
            rs.getString("to_address"),
            rs.getString("subject"),
            rs.getString("body"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
