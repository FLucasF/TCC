package com.pousada.persistence;

import com.pousada.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void save(Connection c, Message message) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, message.channel());
            ps.setString(2, message.to());
            ps.setString(3, message.subject());
            ps.setString(4, message.body());
            ps.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
            return messages;
        }
    }
}
