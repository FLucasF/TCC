package com.pousadamarealta.repository;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.SentMessage;
import com.pousadamarealta.messaging.MessageStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.setString(5, status.name());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<SentMessage> findAll() {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {

            List<SentMessage> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new SentMessage(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    MessageStatus.valueOf(rs.getString("status"))
                ));
            }
            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
    }
}
