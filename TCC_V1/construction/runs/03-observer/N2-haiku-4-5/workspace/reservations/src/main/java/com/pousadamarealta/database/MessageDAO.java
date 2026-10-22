package com.pousadamarealta.database;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private static final MessageDAO INSTANCE = new MessageDAO();

    private MessageDAO() {
    }

    public static MessageDAO getInstance() {
        return INSTANCE;
    }

    public void insertMessage(Message message) throws SQLException {
        String sql = "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getChannel().getCode());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.executeUpdate();
        }
    }

    public List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, to_address, subject, body FROM messages ORDER BY id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message(
                MessageChannel.valueOf(rs.getString("channel")),
                rs.getString("to_address"),
                rs.getString("subject"),
                rs.getString("body")
        );
        message.setId(rs.getLong("id"));
        return message;
    }
}
