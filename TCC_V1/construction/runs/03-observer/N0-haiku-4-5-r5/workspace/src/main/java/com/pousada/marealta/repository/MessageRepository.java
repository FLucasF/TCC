package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.Message.Channel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public Message save(Channel channel, String to, String subject, String body) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, to_address, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Message(id, channel, to, subject, body);
                }
            }
        }

        return null;
    }

    public List<Message> findAll() throws SQLException {
        String sql = "SELECT id, channel, to_address, subject, body FROM messages";
        List<Message> messages = new ArrayList<>();

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    Channel.valueOf(rs.getString("channel")),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }

        return messages;
    }
}
