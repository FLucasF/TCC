package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    public void save(String channel, String to, String subject, String body) {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY created_at";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
            return messages;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
