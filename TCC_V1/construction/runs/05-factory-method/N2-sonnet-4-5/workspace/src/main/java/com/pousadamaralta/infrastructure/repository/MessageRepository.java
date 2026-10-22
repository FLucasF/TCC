package com.pousadamaralta.infrastructure.repository;

import com.pousadamaralta.domain.model.Channel;
import com.pousadamaralta.domain.model.Message;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Message save(Channel channel, String to, String subject, String body) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Message(id, channel, to, subject, body);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    Channel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
