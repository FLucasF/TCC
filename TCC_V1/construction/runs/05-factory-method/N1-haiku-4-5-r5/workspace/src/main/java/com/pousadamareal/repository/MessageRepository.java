package com.pousadamareal.repository;

import com.pousadamareal.db.Database;
import com.pousadamareal.model.Message;

import java.sql.*;
import java.util.*;

public class MessageRepository {

    public static Message save(Message message) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id",
                 Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.channel());
            stmt.setString(2, message.to());
            stmt.setString(3, message.subject());
            stmt.setString(4, message.body());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Message(id, message.channel(), message.to(), message.subject(), message.body());
                }
            }
        }
        throw new SQLException("Falha ao salvar mensagem");
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }
        return messages;
    }
}
