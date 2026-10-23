package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }
        }

        return message;
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        }

        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setChannel(MessageChannel.valueOf(rs.getString("channel")));
        message.setTo(rs.getString("recipient"));
        message.setSubject(rs.getString("subject"));
        message.setBody(rs.getString("body"));
        return message;
    }
}
