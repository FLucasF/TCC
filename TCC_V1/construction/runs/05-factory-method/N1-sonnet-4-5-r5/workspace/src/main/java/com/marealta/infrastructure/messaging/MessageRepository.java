package com.marealta.infrastructure.messaging;

import com.marealta.domain.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Connection connection;

    public MessageRepository(Connection connection) {
        this.connection = connection;
    }

    public Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        }

        return message;
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
