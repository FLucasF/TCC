package pousada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas (por enquanto nada sai de verdade). */
public final class MessageRepository {
    public void add(Connection c, Message m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public List<Message> all(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return list;
        }
    }
}
