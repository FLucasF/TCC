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
        if (!isValidEmail(to)) {
            return addBlockedMessage(channel, to, subject, body);
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (testMode) {
            finalTo = "testes@pousadamarealta.com.br";
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = addMessage(channel, finalTo, finalSubject, finalBody, "SENT");

        if (!testMode && isValidEmail(to)) {
            addMessage(channel, "arquivo@pousadamarealta.com.br", "[Cópia] " + subject, body, "SENT");
        }

        return sent;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || email.indexOf('@', atIndex + 1) >= 0) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }

    private Message addBlockedMessage(String channel, String to, String subject, String body) {
        return addMessage(channel, to, subject, body, "BLOCKED");
    }

    private Message addMessage(String channel, String to, String subject, String body, String status) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to);
            insert.setString(3, subject);
            insert.setString(4, body);
            insert.setString(5, status);
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body, status);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
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
