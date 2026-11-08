package com.marealta.service;

import com.marealta.database.Database;
import com.marealta.domain.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    public static void sendEmail(String to, String subject, String body) throws SQLException {
        String sql = "INSERT INTO messages (channel, \"to\", subject, body) VALUES ('EMAIL', ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, to);
            stmt.setString(2, subject);
            stmt.setString(3, body);
            stmt.executeUpdate();
        }
    }

    public static List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Message msg = new Message(
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                msg.id = rs.getInt("id");
                messages.add(msg);
            }
        }
        return messages;
    }
}
