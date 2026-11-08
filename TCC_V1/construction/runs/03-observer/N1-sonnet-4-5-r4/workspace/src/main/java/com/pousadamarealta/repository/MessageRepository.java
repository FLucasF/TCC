package com.pousadamarealta.repository;

import com.pousadamarealta.domain.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Connection connection;

    public MessageRepository(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(20) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject TEXT NOT NULL,
                body TEXT NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    public Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }
        }

        return message;
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message message = new Message(
            rs.getString("channel"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body")
        );
        message.setId(rs.getLong("id"));
        return message;
    }
}
