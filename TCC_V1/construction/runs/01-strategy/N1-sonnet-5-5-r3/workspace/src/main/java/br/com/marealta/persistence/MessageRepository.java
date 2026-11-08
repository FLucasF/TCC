package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.marealta.domain.Message;

/** A lista de mensagens enviadas (por enquanto, só guardadas). */
public class MessageRepository {
    public void insert(Connection connection, Message message) throws SQLException {
        String sql = "INSERT INTO messages (channel, to_addr, subject, body) VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, message.channel());
            statement.setString(2, message.to());
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.executeUpdate();
        }
    }

    public List<Message> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, channel, to_addr, subject, body FROM messages ORDER BY id");
             ResultSet rows = statement.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rows.next()) {
                result.add(new Message(rows.getLong("id"), rows.getString("channel"), rows.getString("to_addr"),
                        rows.getString("subject"), rows.getString("body")));
            }
            return result;
        }
    }
}
