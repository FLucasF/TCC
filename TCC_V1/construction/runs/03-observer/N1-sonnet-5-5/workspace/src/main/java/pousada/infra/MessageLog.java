package pousada.infra;

import pousada.app.Message;
import pousada.app.MessageSender;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas, guardada no banco. */
public class MessageLog implements MessageSender {
    private final Database db;

    public MessageLog(Database db) {
        this.db = db;
    }

    @Override
    public void send(Message.Channel channel, String to, String subject, String body) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
                ps.setString(1, channel.name());
                ps.setString(2, to);
                ps.setString(3, subject);
                ps.setString(4, body);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<Message> all() {
        return db.inTransaction(c -> {
            List<Message> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
            }
            return result;
        });
    }
}
