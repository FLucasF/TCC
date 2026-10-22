package com.posadamareaita.repository;

import com.posadamareaita.database.Database;
import com.posadamareaita.domain.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static void save(Message message) throws SQLException {
        String sql = """
                INSERT INTO messages (channel, recipient, subject, body)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.executeUpdate();

            var generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                message.setId(generatedKeys.getLong(1));
            }
        }
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";
        try (Connection conn = Database.getDataSource().getConnection();
             var stmt = conn.createStatement()) {
            var resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                messages.add(mapMessage(resultSet));
            }
        }
        return messages;
    }

    private static Message mapMessage(ResultSet rs) throws SQLException {
        Message msg = new Message(
                rs.getString("channel"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getString("body")
        );
        msg.setId(rs.getLong("id"));
        return msg;
    }
}
