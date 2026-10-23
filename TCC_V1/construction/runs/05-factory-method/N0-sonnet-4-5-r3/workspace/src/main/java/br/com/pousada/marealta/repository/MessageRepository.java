package br.com.pousada.marealta.repository;

import br.com.pousada.marealta.model.Message;
import br.com.pousada.marealta.util.DatabaseInitializer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DatabaseInitializer db;

    public MessageRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public Message create(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                message.setId(rs.getLong(1));
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar mensagem", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            List<Message> messages = new ArrayList<>();

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(rs.getString("channel"));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                messages.add(message);
            }

            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar mensagens", e);
        }
    }
}
