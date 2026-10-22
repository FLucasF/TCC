package com.pousada.marealta.repository;

import com.pousada.marealta.database.Database;
import com.pousada.marealta.model.Message;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public static void save(Message msg) throws SQLException {
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement(
                """
                INSERT INTO messages (channel, "to", subject, body) VALUES (?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, msg.getChannel());
            stmt.setString(2, msg.getTo());
            stmt.setString(3, msg.getSubject());
            stmt.setString(4, msg.getBody());

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    msg.setId(rs.getLong(1));
                }
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id");
            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        }
        return messages;
    }

    private static Message mapRow(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
