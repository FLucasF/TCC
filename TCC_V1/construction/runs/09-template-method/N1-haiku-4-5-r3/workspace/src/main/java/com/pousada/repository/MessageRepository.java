package com.pousada.repository;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static void save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                message.setId(keys.getLong(1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public static List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Message msg = new Message(
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                msg.setId(rs.getLong("id"));
                messages.add(msg);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find messages", e);
        }
        return messages;
    }
}
