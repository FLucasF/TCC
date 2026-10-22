package com.posadamarealta.repository;

import com.posadamarealta.model.Message;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final DataSource ds;

    public MessageRepository(DataSource ds) {
        this.ds = ds;
    }

    public void save(String channel, String to, String subject, String body) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        );
    }
}
