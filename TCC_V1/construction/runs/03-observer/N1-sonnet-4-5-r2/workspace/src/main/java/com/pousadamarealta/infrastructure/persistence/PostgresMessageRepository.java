package com.pousadamarealta.infrastructure.persistence;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageChannel;
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
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                message.setId(rs.getLong("id"));
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
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
            return messages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }
    }
}
