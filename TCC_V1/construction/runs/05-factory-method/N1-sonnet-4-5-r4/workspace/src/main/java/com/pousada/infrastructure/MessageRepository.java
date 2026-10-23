package com.pousada.infrastructure;

import com.pousada.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message(
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                message.setId(rs.getLong("id"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
