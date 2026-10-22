package br.com.marealta.infra;

import br.com.marealta.domain.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas: por enquanto nada sai de verdade, só fica registrado aqui. */
public class MessageRepository {

    public void add(Connection connection, Message message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, message.channel());
            statement.setString(2, message.to());
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.executeUpdate();
        }
    }

    public List<Message> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rows = statement.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rows.next()) {
                messages.add(new Message(rows.getLong("id"), rows.getString("channel"),
                        rows.getString("recipient"), rows.getString("subject"), rows.getString("body")));
            }
            return messages;
        }
    }
}
