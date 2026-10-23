package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageChannel;
import com.pousadamarealta.domain.model.MessageStatus;
import com.pousadamarealta.domain.repository.MessageRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresMessageRepository implements MessageRepository {

    private final DataSource dataSource;

    public PostgresMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setId(keys.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setChannel(MessageChannel.valueOf(rs.getString("channel")));
        m.setTo(rs.getString("recipient"));
        m.setSubject(rs.getString("subject"));
        m.setBody(rs.getString("body"));
        m.setStatus(MessageStatus.valueOf(rs.getString("status")));
        return m;
    }
}
