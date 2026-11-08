package com.pousadamarealta.db;

import com.pousadamarealta.model.Message;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static long insert(String channel, String to, String subject, String body) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                 "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages")) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
