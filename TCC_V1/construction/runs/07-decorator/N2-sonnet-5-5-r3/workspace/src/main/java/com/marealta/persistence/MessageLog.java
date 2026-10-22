package com.marealta.persistence;

import com.marealta.mail.Channel;
import com.marealta.mail.Message;
import com.marealta.mail.MessageStatus;
import com.marealta.mail.Outbox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** The list of sent messages. Emails do not leave the system yet: this is the innermost Outbox. */
public final class MessageLog implements Outbox {
    private final Database database;

    public MessageLog(Database database) {
        this.database = database;
    }

    @Override
    public Message send(Message message) {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, message.channel().name());
                ps.setString(2, message.to());
                ps.setString(3, message.subject());
                ps.setString(4, message.body());
                ps.setString(5, message.status().name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return message.withId(keys.getLong("id"));
                }
            }
        });
    }

    public List<Message> all() {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                            MessageStatus.valueOf(rs.getString("status"))));
                }
                return list;
            }
        });
    }
}
