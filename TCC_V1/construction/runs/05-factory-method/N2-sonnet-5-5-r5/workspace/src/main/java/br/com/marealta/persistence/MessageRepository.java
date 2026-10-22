package br.com.marealta.persistence;

import br.com.marealta.domain.Message;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    public void add(Message m) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
                ps.setString(1, m.channel().name());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                return ps.executeUpdate();
            }
        });
    }

    public List<Message> findAll() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
                return list;
            }
        });
    }
}
