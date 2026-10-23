package br.com.pousada.repository;

import br.com.pousada.domain.Message;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = dataSource.getConnection()) {
            String createTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    recipient VARCHAR(255) NOT NULL,
                    subject VARCHAR(500) NOT NULL,
                    body TEXT NOT NULL
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createTable)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize messages table", e);
        }
    }

    public void save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.execute();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }
}
