package com.pousadamareaalta.repository;

import com.pousadamareaalta.model.Message;

import java.sql.*;
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

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }

            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setChannel(rs.getString("channel"));
        m.setTo(rs.getString("recipient"));
        m.setSubject(rs.getString("subject"));
        m.setBody(rs.getString("body"));
        return m;
    }
}
