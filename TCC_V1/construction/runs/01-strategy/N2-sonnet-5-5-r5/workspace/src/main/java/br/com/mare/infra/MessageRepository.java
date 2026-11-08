package br.com.mare.infra;

import br.com.mare.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas: por enquanto nada sai de verdade. */
public class MessageRepository {
    public void save(Connection c, String channel, String to, String subject, String body) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, channel);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return list;
        }
    }
}
