package com.marealta.db;

import com.marealta.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient_email, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, message.getChannel());
            pstmt.setString(2, message.getTo());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getBody());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving message", e);
            throw new RuntimeException(e);
        }

        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient_email, subject, body FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Message message = new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient_email"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            logger.error("Error finding all messages", e);
            throw new RuntimeException(e);
        }

        return messages;
    }
}
