package com.pousada.services;

import com.pousada.database.Database;
import com.pousada.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final Database db;

    public MessageService(Database db) {
        this.db = db;
    }

    public Message sendMessage(String channel, String to, String subject, String body) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?) " +
                             "RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return getMessageById(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar mensagem", e);
        }
        return null;
    }

    public Message getMessageById(long id) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, channel, \"to\", subject, body FROM messages WHERE id = " + id;
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return new Message(
                        rs.getLong("id"),
                        rs.getString("channel"),
                        rs.getString("to"),
                        rs.getString("subject"),
                        rs.getString("body")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar mensagem", e);
        }
        return null;
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                messages.add(new Message(
                        rs.getLong("id"),
                        rs.getString("channel"),
                        rs.getString("to"),
                        rs.getString("subject"),
                        rs.getString("body")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar mensagens", e);
        }
        return messages;
    }
}
