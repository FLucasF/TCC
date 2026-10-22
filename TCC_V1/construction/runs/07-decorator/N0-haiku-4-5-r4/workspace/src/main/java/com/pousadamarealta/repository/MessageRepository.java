package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static void save(Message message) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.channel);
            stmt.setString(2, message.to);
            stmt.setString(3, message.subject);
            stmt.setString(4, message.body);
            stmt.setString(5, message.status);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.id = rs.getLong(1);
                }
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id")) {
            while (rs.next()) {
                Message m = new Message();
                m.id = rs.getLong("id");
                m.channel = rs.getString("channel");
                m.to = rs.getString("recipient");
                m.subject = rs.getString("subject");
                m.body = rs.getString("body");
                m.status = rs.getString("status");
                messages.add(m);
            }
        }
        return messages;
    }
}
