package com.pousadamarealta.repository;

import com.pousadamarealta.messaging.MessageChannel;
import com.pousadamarealta.messaging.MessageStatus;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.SentMessage;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMessageRepository implements MessageRepository {
    private final DataSource dataSource;

    public JdbcMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(OutgoingMessage message, MessageStatus status) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.channel().name());
            stmt.setString(2, message.to());
            stmt.setString(3, message.subject());
            stmt.setString(4, message.body());
            stmt.setString(5, status.name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public List<SentMessage> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<SentMessage> messages = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

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

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
