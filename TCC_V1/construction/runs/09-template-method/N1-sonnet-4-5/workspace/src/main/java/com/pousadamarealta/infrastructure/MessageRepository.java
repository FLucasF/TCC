package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.Message;

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

    public Message save(Message message) {
        String query = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar mensagem", e);
        }
    }

    public List<Message> findAll() {
        String query = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar mensagens", e);
        }

        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String channel = rs.getString("channel");
        String recipient = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");

        return new Message(id, channel, recipient, subject, body);
    }
}
