package br.com.marealta.mail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** "Envia" mensagens: aplica a política e guarda o resultado na lista de mensagens. */
public final class Mailer {
    public static final String CHANNEL = "EMAIL";
    private final MailSettings settings;
    private final MessagePolicy policy;

    public Mailer(MailSettings settings) {
        this.settings = settings;
        this.policy = new MessagePolicy(settings);
    }

    public void send(Connection c, String to, String subject, String body) throws SQLException {
        Instant now = Instant.now();
        Instant dayStart = now.atZone(settings.zone()).toLocalDate().atStartOfDay(settings.zone()).toInstant();
        // Evita corrida entre mensagens para o mesmo hóspede no limite diário.
        try (PreparedStatement lock = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            lock.setLong(1, 7071);
            lock.execute();
        }
        List<Outgoing> out = policy.apply(to, subject, body, now, addr -> {
            try {
                return sentSince(c, addr, dayStart);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages(channel, to_addr, subject, body, status, guest_address, created_at) VALUES (?,?,?,?,?,?,?)")) {
            for (Outgoing o : out) {
                ps.setString(1, CHANNEL);
                ps.setString(2, o.to());
                ps.setString(3, o.subject());
                ps.setString(4, o.body());
                ps.setString(5, o.status());
                ps.setString(6, o.guestAddress());
                ps.setTimestamp(7, Timestamp.from(now));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static int sentSince(Connection c, String guestAddress, Instant since) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM messages WHERE guest_address = ? AND status = 'SENT' AND created_at >= ?")) {
            ps.setString(1, guestAddress);
            ps.setTimestamp(2, Timestamp.from(since));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static List<Message> list(Connection c) throws SQLException {
        List<Message> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, to_addr, subject, body, status FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6)));
            }
        }
        return result;
    }
}
