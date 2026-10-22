package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class MessageService {
    public record Message(long id, String channel, String to, String subject, String body, String status) {}

    private final MessagePolicy policy;
    private final Clock clock;

    public MessageService(MessagePolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    /** Envia (ou bloqueia) uma mensagem dentro da transação da operação que a originou. */
    public void send(Connection c, String to, String subject, String body) throws SQLException {
        // o contador trava até o fim da transação: serializa envios, mantém a cópia colada na original
        // e faz o limite diário contar certo
        long nextId = nextId(c);
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        List<MessagePolicy.Out> outs = policy.process(
                new MessagePolicy.Draft(to, subject, body),
                now.toLocalTime(),
                addr -> sentToday(c, addr, today));
        for (int i = 0; i < outs.size(); i++) {
            MessagePolicy.Out o = outs.get(i);
            long id = i == 0 ? nextId : nextId(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages(id, channel, to_addr, subject, body, status, original_to, copy, sent_on) "
                            + "VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setLong(1, id);
                ps.setString(2, "EMAIL");
                ps.setString(3, o.to());
                ps.setString(4, o.subject());
                ps.setString(5, o.body());
                ps.setString(6, o.blocked() ? "BLOCKED" : "SENT");
                ps.setString(7, o.originalTo().toLowerCase());
                ps.setBoolean(8, o.copy());
                ps.setObject(9, today);
                ps.executeUpdate();
            }
        }
    }

    public List<Message> list(Connection c) throws SQLException {
        List<Message> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, to_addr, subject, body, status FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6)));
            }
        }
        return out;
    }

    static long nextId(Connection c) throws SQLException {
        return Ids.next(c, "messages");
    }

    private int sentToday(Connection c, String addr, LocalDate today) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM messages WHERE original_to = ? AND sent_on = ? AND status = 'SENT' AND NOT copy")) {
            ps.setString(1, addr.toLowerCase());
            ps.setObject(2, today);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
