package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Message save(Message message) {
        String sql = """
                INSERT INTO messages (channel, recipient, subject, body, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all messages", e);
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
