package hotel.messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Every message the inn sent or blocked, so the front desk can check what each guest received. */
public final class SentMessages {

    private final DataSource dataSource;

    public SentMessages(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Message add(String channel, String to, String guestAddress, String subject, String body, MessageStatus status,
            boolean archiveCopy, OffsetDateTime at) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status, guest_address, "
                                + "archive_copy, sent_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to);
            insert.setString(3, subject);
            insert.setString(4, body);
            insert.setString(5, status.name());
            insert.setString(6, guestAddress);
            insert.setBoolean(7, archiveCopy);
            insert.setObject(8, at);
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body, status);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
        }
    }

    /** How many messages actually went to this guest on that day (archive copies and blocked ones do not count). */
    int countSentToGuestOn(String guestAddress, LocalDate day, ZoneId zone) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement count = connection.prepareStatement(
                        "SELECT count(*) FROM messages WHERE guest_address = ? AND status = 'SENT' "
                                + "AND NOT archive_copy AND sent_at >= ? AND sent_at < ?")) {
            count.setString(1, guestAddress);
            count.setObject(2, day.atStartOfDay(zone).toOffsetDateTime());
            count.setObject(3, day.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
            try (ResultSet result = count.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not count the messages to " + guestAddress, failure);
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
