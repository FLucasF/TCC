package br.com.pousadamarealta.domain.message;

import br.com.pousadamarealta.database.DatabaseSetup;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DatabaseSetup database;

    public MessageRepository(DatabaseSetup database) {
        this.database = database;
    }

    public Message save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, to_address, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        }

        return message;
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, to_address, subject, body FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(new Message(
                        rs.getLong("id"),
                        MessageChannel.valueOf(rs.getString("channel")),
                        rs.getString("to_address"),
                        rs.getString("subject"),
                        rs.getString("body")
                ));
            }
        }

        return messages;
    }
}
