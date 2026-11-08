package br.com.marealta.infra;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.service.MessageStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMessageStore implements MessageStore {
    private final Database db;

    public JdbcMessageStore(Database db) {
        this.db = db;
    }

    @Override
    public Message save(OutgoingMessage m) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    @Override
    public List<Message> findAll() {
        try (PreparedStatement ps = db.connection()
                .prepareStatement("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
