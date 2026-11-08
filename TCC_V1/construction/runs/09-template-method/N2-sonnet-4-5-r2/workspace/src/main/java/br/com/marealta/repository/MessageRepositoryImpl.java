package br.com.marealta.repository;

import br.com.marealta.domain.Message;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepositoryImpl implements MessageRepository {
    private final DataSource dataSource;

    public MessageRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Message save(Message message) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                long id = rs.getLong(1);
                return new Message(id, message.getChannel(), message.getTo(),
                        message.getSubject(), message.getBody());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public List<Message> findAll() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {
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
            throw new RuntimeException("Failed to find all messages", e);
        }
    }
}
