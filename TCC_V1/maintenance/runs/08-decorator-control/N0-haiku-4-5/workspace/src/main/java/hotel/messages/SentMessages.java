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

    public SentMessages(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Message add(String channel, String to, String subject, String body) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, to);
            insert.setString(3, subject);
            insert.setString(4, body);
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(generated.getLong("id"), channel, to, subject, body);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + to, failure);
        }
    }

    public List<Message> all() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                ResultSet rows = select.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rows.next()) {
                messages.add(new Message(
                        rows.getLong("id"),
                        rows.getString("channel"),
                        rows.getString("recipient"),
                        rows.getString("subject"),
                        rows.getString("body")));
            }
            return messages;
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read the sent messages", failure);
        }
    }
}
