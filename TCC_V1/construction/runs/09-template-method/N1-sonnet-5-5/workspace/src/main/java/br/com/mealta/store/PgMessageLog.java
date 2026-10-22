package br.com.mealta.store;

import br.com.mealta.domain.Channel;
import br.com.mealta.domain.Message;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PgMessageLog implements MessageLog {
    private final Database db;

    public PgMessageLog(Database db) {
        this.db = db;
    }

    @Override
    public Message add(Message m) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, m.channel().name());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Message(keys.getLong("id"), m.channel(), m.to(), m.subject(), m.body());
                }
            }
        });
    }

    @Override
    public List<Message> all() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
                return messages;
            }
        });
    }
}
