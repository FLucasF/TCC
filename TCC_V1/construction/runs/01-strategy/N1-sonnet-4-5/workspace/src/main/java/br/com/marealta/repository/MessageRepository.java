package br.com.marealta.repository;

import br.com.marealta.domain.Message;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initSchema() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                recipient VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                body TEXT NOT NULL
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create messages table", e);
        }
    }

    public Message save(Message message) {
        String insert = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String query = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                messages.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }

        return messages;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String channel = rs.getString("channel");
        String recipient = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");
        return new Message(id, channel, recipient, subject, body);
    }
}
