package com.pousadamareaalta.database;

import com.pousadamareaalta.domain.Message;
import java.sql.*;
import java.util.*;

public class MessageRepository {
    private static final String INSERT_SQL = """
        INSERT INTO messages (channel, to_email, subject, body)
        VALUES (?, ?, ?, ?)
        RETURNING id
        """;

    public static long create(String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement stmt = Database.getConnection().prepareStatement(INSERT_SQL)) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        String sql = """
            SELECT id, channel, to_email, subject, body
            FROM messages
            ORDER BY id
            """;

        List<Message> messages = new ArrayList<>();
        try (Statement stmt = Database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("to_email"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
