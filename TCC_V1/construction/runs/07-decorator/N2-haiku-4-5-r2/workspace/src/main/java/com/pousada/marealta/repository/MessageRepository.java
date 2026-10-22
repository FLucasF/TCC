package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public Message save(Message message) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                INSERT INTO messages (channel, to_address, subject, body, status)
                VALUES (?, ?, ?, ?, ?)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, message.getChannel().name());
                stmt.setString(2, message.getTo());
                stmt.setString(3, message.getSubject());
                stmt.setString(4, message.getBody());
                stmt.setString(5, message.getStatus().name());

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setId(rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao salvar mensagem", e);
            throw new RuntimeException(e);
        }
        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                SELECT id, channel, to_address, subject, body, status
                FROM messages ORDER BY id
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao listar mensagens", e);
        }
        return messages;
    }

    private Message mapRow(ResultSet rs) throws Exception {
        Message m = new Message(
            Message.Channel.valueOf(rs.getString("channel")),
            rs.getString("to_address"),
            rs.getString("subject"),
            rs.getString("body")
        );
        m.setId(rs.getInt("id"));
        m.setStatus(Message.Status.valueOf(rs.getString("status")));
        return m;
    }
}
