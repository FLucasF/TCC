package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.db.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    public static void sendMessage(String channel, String recipient, String subject, String body) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = """
                INSERT INTO messages (channel, recipient, subject, body)
                VALUES (?, ?, ?, ?)
                """;

            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, channel);
            stmt.setString(2, recipient);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        }
    }

    public static List<Message> getAllMessages() throws SQLException {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM messages ORDER BY id";
            var stmt = conn.prepareStatement(sql);

            List<Message> messages = new ArrayList<>();
            var rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(resultSetToMessage(rs));
            }
            return messages;
        }
    }

    private static Message resultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            rs.getString("channel"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
