package br.com.marealta.persistence;

import br.com.marealta.app.Channel;
import br.com.marealta.app.Message;
import br.com.marealta.app.MessageRepository;
import br.com.marealta.app.Notification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageRepository implements MessageRepository {
    private final DataSource dataSource;

    public JdbcMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Message save(Notification n) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.channel().name());
            ps.setString(2, n.to());
            ps.setString(3, n.subject());
            ps.setString(4, n.body());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Message(keys.getLong("id"), n.channel(), n.to(), n.subject(), n.body());
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> findAll() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
