package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, to_address, subject, body)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getLong(1)).get();
                }
            }
        }

        return message;
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT * FROM messages ORDER BY id";

        List<Message> messages = new ArrayList<>();
        try (Statement stmt = Database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }

        return messages;
    }

    public java.util.Optional<Message> findById(long id) throws SQLException {
        String sql = "SELECT * FROM messages WHERE id = ?";

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapRow(rs));
                }
            }
        }

        return java.util.Optional.empty();
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
                rs.getLong("id"),
                rs.getString("channel"),
                rs.getString("to_address"),
                rs.getString("subject"),
                rs.getString("body")
        );
    }
}
