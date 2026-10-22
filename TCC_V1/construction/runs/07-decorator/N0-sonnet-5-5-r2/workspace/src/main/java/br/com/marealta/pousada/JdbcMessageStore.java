package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.Message;
import br.com.marealta.pousada.Domain.MessageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens no PostgreSQL, usando a conexão (transação) recebida. */
public class JdbcMessageStore implements MessageDispatcher.Store {
    private final Connection c;

    public JdbcMessageStore(Connection c) { this.c = c; }

    @Override
    public Message save(MessageDispatcher.Draft d, MessageStatus status, String guestKey) {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages(channel, recipient, subject, body, status, guest_key) VALUES (?,?,?,?,?,?) RETURNING id")) {
            ps.setString(1, d.channel());
            ps.setString(2, d.to());
            ps.setString(3, d.subject());
            ps.setString(4, d.body());
            ps.setString(5, status.name());
            ps.setString(6, guestKey);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Message(rs.getLong(1), d.channel(), d.to(), d.subject(), d.body(), status);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int countSent(String guestKey, Instant from, Instant to) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM messages WHERE guest_key = ? AND status = 'SENT' AND created_at >= ? AND created_at < ?")) {
            ps.setString(1, guestKey);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<Message> listAll(Connection c) throws SQLException {
        List<Message> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), MessageStatus.valueOf(rs.getString(6))));
            }
        }
        return out;
    }
}
