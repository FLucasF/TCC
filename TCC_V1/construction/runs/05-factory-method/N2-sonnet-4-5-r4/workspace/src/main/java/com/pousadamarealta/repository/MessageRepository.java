package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Message;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "id SERIAL PRIMARY KEY, " +
                "channel VARCHAR(20) NOT NULL, " +
                "recipient VARCHAR(255) NOT NULL, " +
                "subject TEXT NOT NULL, " +
                "body TEXT NOT NULL)"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize messages table", e);
        }
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.channel());
            stmt.setString(2, message.to());
            stmt.setString(3, message.subject());
            stmt.setString(4, message.body());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return message.withId(keys.getLong(1));
                }
                throw new SQLException("Failed to get generated ID");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
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
