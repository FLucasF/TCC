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

    Message add(String channel, OutgoingMessage message) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            insert.setString(1, channel);
            insert.setString(2, message.to());
            insert.setString(3, message.subject());
            insert.setString(4, message.body());
            insert.setString(5, message.status().name());
            try (ResultSet generated = insert.executeQuery()) {
                generated.next();
                return new Message(
                        generated.getLong("id"),
                        channel,
                        message.to(),
                        message.subject(),
                        message.body(),
                        message.status());
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not keep the message to " + message.to(), failure);
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
