package br.com.pousadamarealta.repository;

import br.com.pousadamarealta.domain.Message;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, destination, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                message.setId(rs.getLong(1));
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT id, channel, destination, subject, body FROM messages ORDER BY id";

        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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
        return new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("destination"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
