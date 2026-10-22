package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageRepository implements MessageRepository {
    private final Database db;

    public JdbcMessageRepository(Database db) {
        this.db = db;
    }

    public Message save(Message m) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id")) {
            ps.setString(1, m.channel());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Message(rs.getLong(1), m.channel(), m.to(), m.subject(), m.body());
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        List<Message> all = new ArrayList<>();
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                all.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return all;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
