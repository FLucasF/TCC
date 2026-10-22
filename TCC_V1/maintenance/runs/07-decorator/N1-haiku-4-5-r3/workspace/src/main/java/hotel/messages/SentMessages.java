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
        this(dataSource, "true".equals(System.getenv("HOTEL_TEST_MODE")));
    }

    SentMessages(DataSource dataSource, boolean testMode) {
        this.dataSource = dataSource;
        this.testMode = testMode;
    }

    Message add(String channel, String to, String subject, String body) {
        if (!isValidEmail(to)) {
            return addBlocked(channel, to, subject, body);
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (testMode) {
            finalTo = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = addSent(channel, finalTo, finalSubject, finalBody);

        if (!testMode) {
            addSent(channel, ARCHIVE_EMAIL, "[Cópia] " + subject, body);
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

    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atCount = 0;
        int atIndex = -1;
        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atIndex = i;
            }
        }
        if (atCount != 1) {
            return false;
        }
        if (atIndex == 0) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
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
