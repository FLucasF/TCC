package br.com.marealta.persistence;

import br.com.marealta.messaging.Channel;
import br.com.marealta.messaging.Message;
import br.com.marealta.messaging.MessageSink;
import br.com.marealta.messaging.MessageStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The list of messages sent (or blocked): the end of the pipeline. */
public final class StoredMessages implements MessageSink {
    private final Database database;

    public StoredMessages(Database database) {
        this.database = database;
    }

    @Override
    public void deliver(Message m) {
        database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, m.channel().name());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                ps.setString(5, m.status().name());
                ps.executeUpdate();
                return null;
            }
        });
    }

    public List<Message> list() {
        return database.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                            MessageStatus.valueOf(rs.getString("status"))));
                }
                return messages;
            }
        });
    }
}
