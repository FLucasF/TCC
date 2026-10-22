package com.mareaalta.infrastructure;

import com.mareaalta.domain.Message;
import java.sql.*;
import java.util.*;

public class MessageRepository {
    public static void save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, message.getChannel());
            pstmt.setString(2, message.getTo());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getBody());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                message.setId(rs.getLong(1));
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Message msg = new Message(
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                msg.setId(rs.getLong("id"));
                messages.add(msg);
            }
        }
        return messages;
    }
}
