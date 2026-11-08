package pousada.repo;

import pousada.domain.Channel;
import pousada.domain.Message;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageRepository implements MessageRepository {
    private final DataSource ds;

    public JdbcMessageRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Message add(Channel channel, String to, String subject, String body) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, channel.name());
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Message(rs.getLong(1), channel, to, subject, body);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> all() {
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Message> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), Channel.valueOf(rs.getString(2)), rs.getString(3),
                        rs.getString(4), rs.getString(5)));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
