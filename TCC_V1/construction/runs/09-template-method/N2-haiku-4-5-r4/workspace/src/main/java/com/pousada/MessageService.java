package com.pousada;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    public static void sendEmail(String to, String subject, String body) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = String.format(
                "INSERT INTO messages (channel, \"to\", subject, body) VALUES ('EMAIL', '%s', '%s', %s)",
                escapeString(to),
                escapeString(subject),
                "$$" + escapeString(body) + "$$"
            );
            stmt.execute(query);
        } catch (SQLException e) {
            throw new RuntimeException("Error sending email", e);
        }
    }

    public static List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id");
            while (rs.next()) {
                Message msg = new Message(
                    rs.getInt("id"),
                    rs.getString("channel"),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                messages.add(msg);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching messages", e);
        }
        return messages;
    }

    private static String escapeString(String value) {
        return value.replace("'", "''").replace("\\", "\\\\");
    }
}
