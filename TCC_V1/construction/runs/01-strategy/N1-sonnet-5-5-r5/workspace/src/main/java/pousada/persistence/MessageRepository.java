package pousada.persistence;

import pousada.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas. */
public class MessageRepository {

    public void insert(Connection c, Message m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, m.channel());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        List<Message> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
        }
        return result;
    }
}
