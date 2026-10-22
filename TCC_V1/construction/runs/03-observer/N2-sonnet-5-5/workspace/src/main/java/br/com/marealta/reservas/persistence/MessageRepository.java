package br.com.marealta.reservas.persistence;

import br.com.marealta.reservas.notification.Message;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    public void insert(String channel, String to, String subject, String body) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao gravar mensagem", e);
        }
    }

    public List<Message> findAll() {
        return db.inTransaction(() -> {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(new Message(rs.getLong("id"), rs.getString("channel"),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
                return messages;
            } catch (SQLException e) {
                throw new DatabaseException("Erro ao listar mensagens", e);
            }
        });
    }
}
