package br.com.marealta.persistence;

import br.com.marealta.mail.Channel;
import br.com.marealta.mail.Message;
import br.com.marealta.mail.MessageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    public void insert(Connection connection, Message message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO messages (channel, to_address, subject, body, status) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, message.channel().name());
            statement.setString(2, message.to());
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.setString(5, message.status().name());
            statement.executeUpdate();
        }
    }

    public List<Message> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, channel, to_address, subject, body, status FROM messages ORDER BY id");
             ResultSet rows = statement.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rows.next()) {
                messages.add(new Message(rows.getLong("id"), Channel.valueOf(rows.getString("channel")),
                        rows.getString("to_address"), rows.getString("subject"), rows.getString("body"),
                        MessageStatus.valueOf(rows.getString("status"))));
            }
            return messages;
        }
    }
}
