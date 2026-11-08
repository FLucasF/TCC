package com.pousada.marealta.db;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.enums.MessageChannel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static Message create(MessageChannel channel, String to, String subject, String body) throws Exception {
        String sql = "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Message(id, channel, to, subject, body);
                }
            }
        }
        return null;
    }

    public static List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages")) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
