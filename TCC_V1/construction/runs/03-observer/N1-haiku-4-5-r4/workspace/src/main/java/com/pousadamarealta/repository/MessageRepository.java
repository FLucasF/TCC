package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public Message create(String channel, String to, String subject, String body) throws Exception {
        String sql = "INSERT INTO messages (channel, to_address, subject, body) " +
            "VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Message(id, channel, to, subject, body);
                }
            }
        }
        throw new Exception("Failed to create message");
    }

    public List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapToMessage(rs));
            }
        }
        return messages;
    }

    private Message mapToMessage(ResultSet rs) throws Exception {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to_address"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
