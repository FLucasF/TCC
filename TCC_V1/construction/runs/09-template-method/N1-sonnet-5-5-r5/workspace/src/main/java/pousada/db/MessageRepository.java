package pousada.db;

import java.util.ArrayList;
import java.util.List;

import pousada.domain.Channel;
import pousada.domain.Message;
import pousada.domain.Notifier;

/** Lista de mensagens enviadas; é também o "envio" por enquanto. */
public class MessageRepository implements Notifier {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    @Override
    public void send(Channel channel, String to, String subject, String body) {
        db.use(connection -> {
            try (var statement = connection.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, channel.name());
                statement.setString(2, to);
                statement.setString(3, subject);
                statement.setString(4, body);
                return statement.executeUpdate();
            }
        });
    }

    public List<Message> all() {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 var rows = statement.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (rows.next()) {
                    messages.add(new Message(rows.getLong("id"), Channel.valueOf(rows.getString("channel")),
                            rows.getString("recipient"), rows.getString("subject"), rows.getString("body")));
                }
                return messages;
            }
        });
    }
}
