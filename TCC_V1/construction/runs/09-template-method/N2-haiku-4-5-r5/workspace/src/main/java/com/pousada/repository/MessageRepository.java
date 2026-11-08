package com.pousada.repository;

import com.pousada.database.Database;
import com.pousada.domain.Message;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static Message create(String channel, String to, String subject, String body) {
        String sql = "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create message", e);
        }

        throw new RuntimeException("Failed to create message: no ID returned");
    }

    public static Message findById(int id) {
        String sql = "SELECT * FROM messages WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapMessage(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find message", e);
        }

        return null;
    }

    public static List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY sent_at DESC";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
            return messages;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    private static Message mapMessage(ResultSet rs) throws SQLException {
        return new Message(
                rs.getInt("id"),
                rs.getString("channel"),
                rs.getString("to"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getTimestamp("sent_at").toLocalDateTime()
        );
    }
}
