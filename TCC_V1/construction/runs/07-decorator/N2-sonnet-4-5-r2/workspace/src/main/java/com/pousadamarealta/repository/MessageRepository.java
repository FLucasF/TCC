package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    private final DatabaseManager dbManager;

    public MessageRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.setString(5, status.name());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Long id = rs.getLong("id");
                MessageChannel channel = MessageChannel.valueOf(rs.getString("channel"));
                String to = rs.getString("recipient");
                String subject = rs.getString("subject");
                String body = rs.getString("body");
                MessageStatus status = MessageStatus.valueOf(rs.getString("status"));

                messages.add(new Message(id, channel, to, subject, body, status));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }

        return messages;
    }
}
