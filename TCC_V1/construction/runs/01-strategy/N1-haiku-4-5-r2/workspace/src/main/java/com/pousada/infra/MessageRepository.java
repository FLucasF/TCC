package com.pousada.infra;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public record Message(long id, String channel, String to, String subject, String body) {}

    public void save(String channel, String to, String subject, String body) throws SQLException {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        }
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
