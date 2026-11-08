package com.marealta.persistence;

import com.marealta.domain.Channel;
import com.marealta.domain.Message;
import com.marealta.domain.MessageOutbox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository implements MessageOutbox {
    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    @Override
    public void send(Channel channel, String to, String subject, String body) {
        db.transaction(() -> {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, channel.name());
                ps.setString(2, to);
                ps.setString(3, subject);
                ps.setString(4, body);
                ps.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new UncheckedSqlException(e);
            }
        });
    }

    public List<Message> findAll() {
        return db.transaction(() -> {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<Message> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
                return list;
            } catch (SQLException e) {
                throw new UncheckedSqlException(e);
            }
        });
    }
}
