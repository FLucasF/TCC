package br.com.marealta.repository;

import br.com.marealta.model.Message;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource dataSource;

    public MessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Message save(Message message) {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                }
            }

            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving message", e);
        }
    }

    public List<Message> findAll() {
        String sql = "SELECT * FROM messages ORDER BY id";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setChannel(rs.getString("channel"));
                message.setTo(rs.getString("recipient"));
                message.setSubject(rs.getString("subject"));
                message.setBody(rs.getString("body"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding messages", e);
        }

        return messages;
    }
}
