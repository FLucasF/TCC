package com.pousadamarealta.database;

import com.pousadamarealta.models.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static Message create(String channel, String recipient, String subject, String body) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, recipient);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return findById(id);
                }
            }
        }
        return null;
    }

    public static Message findById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }
        return messages;
    }

    private static Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            rs.getString("channel"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
