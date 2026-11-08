package br.com.mare.pousada.infra;

import br.com.mare.pousada.domain.Message;
import br.com.mare.pousada.domain.MessageChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas. */
public class MessageRepository {

    public void save(Connection c, Message m) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            s.setString(1, m.channel().name());
            s.setString(2, m.to());
            s.setString(3, m.subject());
            s.setString(4, m.body());
            s.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM messages ORDER BY id");
             ResultSet rs = s.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), MessageChannel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        }
    }
}
