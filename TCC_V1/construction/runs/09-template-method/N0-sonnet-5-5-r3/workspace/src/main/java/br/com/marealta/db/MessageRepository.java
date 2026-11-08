package br.com.marealta.db;

import br.com.marealta.domain.Channel;
import br.com.marealta.domain.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The list of sent messages (e-mails are only recorded here for now). */
public final class MessageRepository {

    public Message add(Connection c, Channel channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, to_addr, subject, body) VALUES (?,?,?,?) RETURNING id")) {
            ps.setString(1, channel.name());
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Message(rs.getLong(1), channel, to, subject, body);
            }
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, to_addr, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("to_addr"), rs.getString("subject"), rs.getString("body")));
            }
            return list;
        }
    }
}
