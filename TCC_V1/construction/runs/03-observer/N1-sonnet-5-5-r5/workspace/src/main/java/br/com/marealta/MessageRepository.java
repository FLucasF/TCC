package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas (por enquanto, só guardadas). */
public class MessageRepository {
    public void saveAll(Connection c, List<Message> messages) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            for (Message m : messages) {
                s.setString(1, m.channel().name());
                s.setString(2, m.to());
                s.setString(3, m.subject());
                s.setString(4, m.body());
                s.addBatch();
            }
            s.executeBatch();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = s.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        }
    }
}
