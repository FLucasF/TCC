package br.com.pousada.repository;

import br.com.pousada.config.DatabaseConfig;
import br.com.pousada.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.channel);
            stmt.setString(2, message.to);
            stmt.setString(3, message.subject);
            stmt.setString(4, message.body);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.id = rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao inserir mensagem", e);
            throw new RuntimeException(e);
        }
        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT id, channel, "to", subject, body
            FROM messages
            ORDER BY id
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar mensagens", e);
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
