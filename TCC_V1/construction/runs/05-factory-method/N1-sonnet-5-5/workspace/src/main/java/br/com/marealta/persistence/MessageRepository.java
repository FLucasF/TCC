package br.com.marealta.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens "enviadas": por enquanto nada sai de verdade, só fica guardado. */
public class MessageRepository {
    public record Message(long id, String channel, String to, String subject, String body) {}

    public void save(Connection connection, String channel, String to, String subject, String body)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        }
    }

    public List<Message> findAll(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
            return messages;
        }
    }
}
