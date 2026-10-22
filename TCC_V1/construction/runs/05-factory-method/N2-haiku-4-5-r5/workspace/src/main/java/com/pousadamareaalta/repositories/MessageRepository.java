package com.pousadamareaalta.repositories;

import com.pousadamareaalta.Database;
import com.pousadamareaalta.models.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public long save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.channel);
            stmt.setString(2, message.to);
            stmt.setString(3, message.subject);
            stmt.setString(4, message.body);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new RuntimeException("Failed to save message");
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT * FROM messages";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(mapResultSet(rs));
            }
        }

        return messages;
    }

    private Message mapResultSet(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.id = rs.getLong("id");
        m.channel = rs.getString("channel");
        m.to = rs.getString("to");
        m.subject = rs.getString("subject");
        m.body = rs.getString("body");
        return m;
    }
}
