package br.com.pousadamarealta.repository.postgres;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.MessageChannel;
import br.com.pousadamarealta.repository.MessageRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PostgresMessageRepository implements MessageRepository {
    private final Database database;

    public PostgresMessageRepository(Database database) {
        this.database = database;
    }

    @Override
    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.channel().name());
            stmt.setString(2, message.to());
            stmt.setString(3, message.subject());
            stmt.setString(4, message.body());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Message(id, message.channel(), message.to(), message.subject(), message.body());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    @Override
    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all messages", e);
        }

        return messages;
    }
}
