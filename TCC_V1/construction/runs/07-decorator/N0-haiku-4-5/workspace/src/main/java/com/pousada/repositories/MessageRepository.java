package com.pousada.repositories;

import com.pousada.database.Database;
import com.pousada.models.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message create(Message message) throws Exception {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }
        }

        return message;
    }

    public List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT id, channel, recipient, subject, body, status
            FROM messages
            ORDER BY id
            """;

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getLong("id"));
                msg.setChannel(rs.getString("channel"));
                msg.setTo(rs.getString("recipient"));
                msg.setSubject(rs.getString("subject"));
                msg.setBody(rs.getString("body"));
                msg.setStatus(rs.getString("status"));
                messages.add(msg);
            }
        }

        return messages;
    }
}
