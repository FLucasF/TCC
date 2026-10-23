package com.pousadamarealta.repository;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;

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

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setId(keys.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding messages", e);
        }

        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
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
