package com.pousada.repository;

import com.pousada.Database;
import com.pousada.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public static Message create(Message message) {
        String sql = """
                INSERT INTO messages (channel, recipient_email, subject, body)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, message.getChannel());
            pstmt.setString(2, message.getTo());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getBody());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return messages;
    }

    private static Message mapMessage(ResultSet rs) throws Exception {
        Message message = new Message(
            rs.getString("channel"),
            rs.getString("recipient_email"),
            rs.getString("subject"),
            rs.getString("body")
        );
        message.setId(rs.getInt("id"));
        return message;
    }
}
