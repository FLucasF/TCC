package com.pousadamarealta.database;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final DatabaseManager dbManager;

    public MessageDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql, new String[]{"id"})) {

            pstmt.setString(1, message.getChannel().name());
            pstmt.setString(2, message.getTo());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getBody());
            pstmt.setString(5, message.getStatus().name());

            pstmt.executeUpdate();

            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(MessageChannel.valueOf(rs.getString("channel")));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                message.setStatus(MessageStatus.valueOf(rs.getString("status")));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
