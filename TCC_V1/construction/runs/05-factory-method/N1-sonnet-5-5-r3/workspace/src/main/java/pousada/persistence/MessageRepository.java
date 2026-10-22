package pousada.persistence;

import pousada.domain.Channel;
import pousada.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens "enviadas" (por enquanto os e-mails só ficam guardados aqui). */
public final class MessageRepository {

    public void insert(Connection conn, Message m) throws SQLException {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        }
    }

    public List<Message> all(Connection conn) throws SQLException {
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
        }
        return list;
    }
}
