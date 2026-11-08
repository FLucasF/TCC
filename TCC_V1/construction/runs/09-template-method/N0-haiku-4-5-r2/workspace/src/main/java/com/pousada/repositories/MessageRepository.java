package com.pousada.repositories;

import com.pousada.Database;
import com.pousada.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public static Message create(String channel, String to, String subject, String body) {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return new Message(id, channel, to, subject, body);
            }
        } catch (SQLException e) {
            logger.error("Error creating message", e);
        }
        return null;
    }

    public static List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id"
            );

            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error listing messages", e);
        }
        return messages;
    }
}
