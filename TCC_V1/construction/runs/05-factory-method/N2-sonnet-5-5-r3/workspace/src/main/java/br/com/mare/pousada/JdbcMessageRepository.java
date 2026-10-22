package br.com.mare.pousada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageRepository implements MessageRepository {
    private final JdbcDatabase db;

    public JdbcMessageRepository(JdbcDatabase db) {
        this.db = db;
    }

    @Override
    public Message add(Channel channel, String to, String subject, String body) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, channel.name());
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Message(keys.getInt(1), channel, to, subject, body);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> findAll() {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> all = new ArrayList<>();
            while (rs.next()) {
                all.add(new Message(rs.getInt("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return all;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
