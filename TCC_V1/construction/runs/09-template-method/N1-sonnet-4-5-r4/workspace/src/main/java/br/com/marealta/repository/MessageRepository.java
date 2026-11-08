package br.com.marealta.repository;

import br.com.marealta.database.Database;
import br.com.marealta.domain.Message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql, new String[]{"id"})) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }

        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id";

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages", e);
        }

        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setChannel(rs.getString("channel"));
        message.setTo(rs.getString("recipient"));
        message.setSubject(rs.getString("subject"));
        message.setBody(rs.getString("body"));
        return message;
    }
}
