package com.pousada.marealta.persistence;

import com.pousada.marealta.model.Message;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    private final DataSource dataSource;

    public MessageDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(Message message) throws SQLException {
        String sql = "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                message.setId(rs.getLong(1));
            }
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, to_address, subject, body FROM messages ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Message msg = new Message(
                    rs.getString("channel"),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                );
                msg.setId(rs.getLong("id"));
                messages.add(msg);
            }
        }

        return messages;
    }
}
