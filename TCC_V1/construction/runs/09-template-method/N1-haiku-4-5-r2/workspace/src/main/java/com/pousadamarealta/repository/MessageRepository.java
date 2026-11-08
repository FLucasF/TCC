package com.pousadamarealta.repository;

import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.model.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public void create(Message message) throws Exception {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        }
    }

    public List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {

            while (rs.next()) {
                messages.add(mapResultSet(rs));
            }
        }
        return messages;
    }

    private Message mapResultSet(ResultSet rs) throws Exception {
        Message message = new Message(
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        );
        message.setId(rs.getLong("id"));
        return message;
    }
}
