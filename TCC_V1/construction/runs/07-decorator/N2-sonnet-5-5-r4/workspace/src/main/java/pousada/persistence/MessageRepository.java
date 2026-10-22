package pousada.persistence;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import pousada.messaging.Message;
import pousada.messaging.MessageSink;
import pousada.messaging.MessageStatus;

/** Lista de mensagens: é também o destino final das mensagens (por enquanto não saem de verdade). */
public final class MessageRepository implements MessageSink {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    @Override
    public Message send(Message m) {
        return db.transaction(c -> {
            try (var ps = c.prepareStatement(
                    "INSERT INTO messages (channel, to_address, subject, body, status) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, m.channel());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                ps.setString(5, m.status().name());
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Message(keys.getLong(1), m.channel(), m.to(), m.subject(), m.body(), m.status());
                }
            }
        });
    }

    public List<Message> all() {
        return db.transaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM messages ORDER BY id");
                 var rs = ps.executeQuery()) {
                List<Message> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("to_address"),
                            rs.getString("subject"), rs.getString("body"),
                            MessageStatus.valueOf(rs.getString("status"))));
                }
                return list;
            }
        });
    }
}
