package com.pousada.maralta.repository;

import com.pousada.maralta.domain.Message;
import com.pousada.maralta.infrastructure.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageRepository implements MessageRepository {
    @Override
    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
