package com.pousada.database;

import com.pousada.model.Message;
import java.sql.Connection;
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
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, destination, subject, body) VALUES (?, ?, ?, ?) " +
                "RETURNING id",
                Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            long id = rs.getLong(1);

            return new Message(id, channel, to, subject, body);
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT id, channel, destination, subject, body FROM messages ORDER BY id");
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("destination"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
