package hotel.messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Every message the inn sent or blocked, so the front desk can check what each guest received. */
public final class SentMessages {

    private final DataSource dataSource;

    public SentMessages(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Message add(
            String channel,
            String to,
            String subject,
            String body,
            MessageStatus status,
            String originalRecipient,
            Instant sentAt) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status, original_recipient, sent_at)"
                                + " VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to == null ? "" : to);
            insert.setString(3, subject);
            insert.setString(4, body);
            insert.setString(5, status.name());
            insert.setString(6, originalRecipient);
            insert.setObject(7, sentAt.atOffset(ZoneOffset.UTC));
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body, status);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
        }
    }

    /** How many messages were really sent to this guest's address in [from, until). */
    int countSentToGuest(String address, Instant from, Instant until) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement count = connection.prepareStatement(
                        "SELECT count(*) FROM messages WHERE status = 'SENT'"
                                + " AND lower(coalesce(original_recipient, recipient)) = lower(?)"
                                + " AND sent_at >= ? AND sent_at < ?")) {
            count.setString(1, address);
            count.setObject(2, from.atOffset(ZoneOffset.UTC));
            count.setObject(3, until.atOffset(ZoneOffset.UTC));
            try (ResultSet row = count.executeQuery()) {
                row.next();
                return row.getInt(1);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not count the messages to " + address, failure);
        }
    }

    public List<Message> all() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id");
                ResultSet rows = select.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rows.next()) {
                messages.add(new Message(
                        rows.getLong("id"),
                        rows.getString("channel"),
                        rows.getString("recipient"),
                        rows.getString("subject"),
                        rows.getString("body"),
                        MessageStatus.valueOf(rows.getString("status"))));
            }
            return messages;
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read the sent messages", failure);
        }
    }
}
