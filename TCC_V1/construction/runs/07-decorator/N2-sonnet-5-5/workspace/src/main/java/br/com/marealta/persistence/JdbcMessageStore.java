package br.com.marealta.persistence;

import br.com.marealta.messaging.Message;
import br.com.marealta.messaging.MessageStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMessageStore implements MessageStore {
    private final Database database;

    public JdbcMessageStore(Database database) { this.database = database; }

    @Override
    public Message save(Message m) {
        try (Connection c = database.open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.setString(5, m.status().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Message(keys.getLong(1), m.channel(), m.to(), m.subject(), m.body(), m.status());
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> all() {
        try (Connection c = database.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                        Message.Status.valueOf(rs.getString("status"))));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
