package hotel.messages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Every message the inn sent, so the front desk can check what each guest received. */
public final class SentMessages {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final DataSource dataSource;
    private final boolean testMode;

    public SentMessages(DataSource dataSource) {
        this(dataSource, "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE")));
    }

    public SentMessages(DataSource dataSource, boolean testMode) {
        this.dataSource = dataSource;
        this.testMode = testMode;
    }

    Message add(String channel, String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return addBlocked(channel, to, subject, body);
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (testMode) {
            actualTo = TEST_EMAIL;
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = addSent(channel, actualTo, actualSubject, actualBody);

        if (!testMode) {
            addArchiveCopy(channel, sent, subject, body);
        }

        return sent;
    }

    private Message addSent(String channel, String to, String subject, String body) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, 'SENT') RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to);
            insert.setString(3, subject);
            insert.setString(4, body);
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body, "SENT");
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
        }
    }

    private Message addBlocked(String channel, String to, String subject, String body) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, 'BLOCKED') RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to);
            insert.setString(3, subject);
            insert.setString(4, body);
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body, "BLOCKED");
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
        }
    }

    private void addArchiveCopy(String channel, Message original, String subject, String body) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, 'SENT')")) {
            insert.setString(1, channel);
            insert.setString(2, ARCHIVE_EMAIL);
            insert.setString(3, "[Cópia] " + subject);
            insert.setString(4, body);
            insert.executeUpdate();
        } catch (SQLException failure) {
            throw new IllegalStateException("could not save the archive copy", failure);
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
                        rows.getString("status")));
            }
            return messages;
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read the sent messages", failure);
        }
    }
}
