package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas. Por enquanto nada sai de verdade: só fica registrado aqui. */
public final class MessageRepository {

    public void record(Connection c, String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            s.setString(1, channel);
            s.setString(2, to);
            s.setString(3, subject);
            s.setString(4, body);
            s.executeUpdate();
        }
    }

    public List<Message> all(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = s.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
            return result;
        }
    }
}
