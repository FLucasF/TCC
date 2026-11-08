package br.marealta.db;

import br.marealta.model.Message;
import br.marealta.model.MessageChannel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final Database db;

    public MessageDAO(Database db) {
        this.db = db;
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, message.getChannel().name());
            pstmt.setString(2, message.getTo());
            pstmt.setString(3, message.getSubject());
            pstmt.setString(4, message.getBody());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                message.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT id, channel, to_address, subject, body FROM messages ORDER BY id");
            while (rs.next()) {
                Message message = new Message(
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                message.setId(rs.getLong("id"));
                messages.add(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }
}
