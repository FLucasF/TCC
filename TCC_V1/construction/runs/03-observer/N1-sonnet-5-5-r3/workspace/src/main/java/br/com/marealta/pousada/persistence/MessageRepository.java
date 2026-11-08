package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.notification.Channel;
import br.com.marealta.pousada.notification.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas; por enquanto nada sai de verdade. */
public class MessageRepository {

    public void saveAll(Connection c, List<Message> messages) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            for (Message m : messages) {
                ps.setString(1, m.channel().name());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        }
    }
}
