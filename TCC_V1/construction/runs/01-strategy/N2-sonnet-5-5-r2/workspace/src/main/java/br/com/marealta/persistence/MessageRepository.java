package br.com.marealta.persistence;

import br.com.marealta.domain.Channel;
import br.com.marealta.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista das mensagens "enviadas" (por enquanto nada sai de verdade). */
public class MessageRepository {
    public void add(Connection c, Message message) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, message.channel().name());
            ps.setString(2, message.to());
            ps.setString(3, message.subject());
            ps.setString(4, message.body());
            ps.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return list;
        }
    }
}
