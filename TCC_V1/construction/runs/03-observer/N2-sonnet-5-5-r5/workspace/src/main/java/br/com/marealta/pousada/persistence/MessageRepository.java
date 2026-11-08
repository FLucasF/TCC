package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas. Por enquanto nada sai de verdade: as mensagens ficam guardadas aqui. */
public class MessageRepository {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    public void send(Message.Channel channel, String to, String subject, String body) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, channel.name());
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
