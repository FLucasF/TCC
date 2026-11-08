package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public void create(String channel, String toAddress, String subject, String body) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, channel);
            stmt.setString(2, toAddress);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        }
    }

    public List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, to_address, subject, body FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("channel"),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
