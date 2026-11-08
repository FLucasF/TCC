package br.com.marealta.pousada.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.com.marealta.pousada.notification.Channel;
import br.com.marealta.pousada.notification.Message;
import br.com.marealta.pousada.notification.MessageLog;

public class JdbcMessageLog implements MessageLog {

    private final Database db;

    public JdbcMessageLog(Database db) {
        this.db = db;
    }

    @Override
    public Message send(Channel channel, String to, String subject, String body) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)";
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, channel.name());
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Message(keys.getLong(1), channel, to, subject, body);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> all() {
        try (Connection c = db.connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM messages ORDER BY id");
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
