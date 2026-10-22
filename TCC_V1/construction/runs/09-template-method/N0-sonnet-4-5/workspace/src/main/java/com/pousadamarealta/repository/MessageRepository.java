package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Message;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getLong("id"));
                m.setChannel(rs.getString("channel"));
                m.setTo(rs.getString("recipient"));
                m.setSubject(rs.getString("subject"));
                m.setBody(rs.getString("body"));
                messages.add(m);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
        return messages;
    }
}
