package com.pousadamarealta.database;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    private final Database database;

    public MessageDao(Database database) {
        this.database = database;
    }

    public Message create(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }

            return message;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages
            ORDER BY id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            MessageChannel.valueOf(rs.getString("channel")),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body"),
            MessageStatus.valueOf(rs.getString("status"))
        );
    }
}
