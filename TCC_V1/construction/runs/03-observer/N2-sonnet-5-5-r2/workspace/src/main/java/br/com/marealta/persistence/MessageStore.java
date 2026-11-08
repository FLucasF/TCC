package br.com.marealta.persistence;

import br.com.marealta.notification.Channel;
import br.com.marealta.notification.Message;
import br.com.marealta.notification.MessageSender;
import java.util.List;

/** A lista de mensagens enviadas, guardada no banco. */
public final class MessageStore implements MessageSender {

    private final Database db;

    public MessageStore(Database db) {
        this.db = db;
    }

    @Override
    public void send(Message message) {
        db.insert("INSERT INTO message (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id", ps -> {
            ps.setString(1, message.channel().name());
            ps.setString(2, message.to());
            ps.setString(3, message.subject());
            ps.setString(4, message.body());
        });
    }

    public List<Message> findAll() {
        return db.query("SELECT id, channel, recipient, subject, body FROM message ORDER BY id", ps -> { },
                rs -> new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
    }
}
