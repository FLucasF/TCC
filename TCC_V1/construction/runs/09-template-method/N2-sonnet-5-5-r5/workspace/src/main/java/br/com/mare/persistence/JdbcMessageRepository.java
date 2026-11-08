package br.com.mare.persistence;

import br.com.mare.domain.Channel;
import br.com.mare.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/** Lista de mensagens enviadas. */
public class JdbcMessageRepository {
    private final DataSource dataSource;

    public JdbcMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Message add(Message m) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Message(keys.getLong("id"), m.channel(), m.to(), m.subject(), m.body());
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM messages ORDER BY id")) {
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
