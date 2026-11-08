package com.pousadamareaita.repository;

import com.pousadamareaita.Database;
import com.pousadamareaita.model.Message;
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
        String sql = String.format(
            "INSERT INTO messages (channel, \"to\", subject, body) " +
            "VALUES ('%s', '%s', '%s', '%s') " +
            "RETURNING id",
            escapeSql(message.getChannel()),
            escapeSql(message.getTo()),
            escapeSql(message.getSubject() != null ? message.getSubject() : ""),
            escapeSql(message.getBody())
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                message.setId(rs.getLong("id"));
                return message;
            }
        } catch (SQLException e) {
            logger.error("Error saving message", e);
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Failed to save message");
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Message message = new Message(
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                message.setId(rs.getLong("id"));
                messages.add(message);
            }
        } catch (SQLException e) {
            logger.error("Error fetching messages", e);
        }

        return messages;
    }

    private String escapeSql(String str) {
        if (str == null) return "";
        return str.replace("'", "''");
    }
}
