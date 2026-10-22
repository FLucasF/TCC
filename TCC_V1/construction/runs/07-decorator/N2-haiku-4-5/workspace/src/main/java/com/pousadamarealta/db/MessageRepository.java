package com.pousadamarealta.db;

import com.pousadamarealta.model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, to_address, subject, body, status) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }
        }
        return message;
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to_address"),
            rs.getString("subject"),
            rs.getString("body"),
            rs.getString("status")
        );
    }
}
