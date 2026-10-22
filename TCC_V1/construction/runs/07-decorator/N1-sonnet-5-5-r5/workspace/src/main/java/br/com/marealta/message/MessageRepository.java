package br.com.marealta.message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void insert(Connection connection, Message message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into messages (channel, recipient, subject, body, status) values (?, ?, ?, ?, ?)")) {
            statement.setString(1, message.channel().name());
            statement.setString(2, message.to());
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.setString(5, message.status().name());
            statement.executeUpdate();
        }
    }

    public List<Message> all(Connection connection) throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select id, channel, recipient, subject, body, status from messages order by id");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                        MessageStatus.valueOf(rs.getString("status"))));
            }
        }
        return messages;
    }
}
