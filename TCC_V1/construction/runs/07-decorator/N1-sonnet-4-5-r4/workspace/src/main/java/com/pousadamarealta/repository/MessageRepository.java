package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                message.setId(rs.getLong(1));
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            List<Message> messages = new ArrayList<>();

            while (rs.next()) {
                messages.add(mapRow(rs));
            }

            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setChannel(MessageChannel.valueOf(rs.getString("channel")));
        message.setTo(rs.getString("recipient"));
        message.setSubject(rs.getString("subject"));
        message.setBody(rs.getString("body"));
        message.setStatus(MessageStatus.valueOf(rs.getString("status")));
        return message;
    }
}
