package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.messaging.Channel;
import br.com.marealta.pousada.messaging.Message;
import br.com.marealta.pousada.messaging.MessageStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    private final Connection connection;

    MessageRepository(Connection connection) {
        this.connection = connection;
    }

    /** Grava as mensagens de um mesmo envio em sequência, sem outras no meio. */
    public List<Message> insertAll(List<Message> messages) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(-1)")) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return messages.stream().map(this::insert).toList();
    }

    public Message insert(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, message.channel().name());
            statement.setString(2, message.to());
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.setString(5, message.status().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return message.withId(keys.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rows.next()) {
                messages.add(new Message(rows.getLong("id"), Channel.valueOf(rows.getString("channel")),
                        rows.getString("recipient"), rows.getString("subject"), rows.getString("body"),
                        MessageStatus.valueOf(rows.getString("status"))));
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
