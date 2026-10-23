package com.pousadamareal.repository;

import com.pousadamareal.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(String channel, String to, String subject, String body) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new Message(rs.getLong(1), channel, to, subject, body);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
        throw new RuntimeException("Failed to save message");
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
    }
}
