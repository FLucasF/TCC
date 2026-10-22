package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final MessageRepository instance = new MessageRepository();

    private MessageRepository() {
    }

    public static MessageRepository getInstance() {
        return instance;
    }

    public void save(String channel, String to, String subject, String body) {
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection()) {
            var rs = conn.createStatement().executeQuery(
                "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id"
            );
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
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
