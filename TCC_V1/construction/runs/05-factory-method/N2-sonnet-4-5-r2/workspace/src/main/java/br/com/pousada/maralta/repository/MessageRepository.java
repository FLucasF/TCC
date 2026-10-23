package br.com.pousada.maralta.repository;

import br.com.pousada.maralta.database.Database;
import br.com.pousada.maralta.domain.Message;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(String channel, String recipient, String subject, String body) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(
                 "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id",
                 java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, channel);
            stmt.setString(2, recipient);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            var rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                return new Message(id, channel, recipient, subject, body);
            }

            throw new RuntimeException("Failed to get generated key");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {

            var rs = stmt.executeQuery();
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
