package br.com.marealto.repository;

import br.com.marealto.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(rs.getString("channel"));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
