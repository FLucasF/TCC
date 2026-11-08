package com.marealata.dao;

import com.marealata.database.Database;
import com.marealata.model.Message;
import java.sql.*;
import java.util.*;

public class MessageDao {
    public static Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient_email, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql,
             Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.channel);
            stmt.setString(2, message.to);
            stmt.setString(3, message.subject);
            stmt.setString(4, message.body);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.id = rs.getLong(1);
                }
            }
        }
        return message;
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT id, channel, recipient_email, subject, body
            FROM messages
            ORDER BY id
            """;

        try (Statement stmt = Database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Message msg = new Message();
                msg.id = rs.getLong("id");
                msg.channel = rs.getString("channel");
                msg.to = rs.getString("recipient_email");
                msg.subject = rs.getString("subject");
                msg.body = rs.getString("body");
                messages.add(msg);
            }
        }
        return messages;
    }
}
