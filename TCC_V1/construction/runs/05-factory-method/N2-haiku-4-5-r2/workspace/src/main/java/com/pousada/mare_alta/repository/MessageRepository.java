package com.pousada.mare_alta.repository;

import com.pousada.mare_alta.database.Database;
import com.pousada.mare_alta.model.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message create(String channel, String to, String subject, String body) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, to_address, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                return new Message(id, channel, to, subject, body);
            }
            throw new SQLException("Failed to create message");
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages ORDER BY id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }
        return messages;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
                rs.getLong("id"),
                rs.getString("channel"),
                rs.getString("to_address"),
                rs.getString("subject"),
                rs.getString("body")
        );
    }
}
