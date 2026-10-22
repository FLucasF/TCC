package com.marealta.pousada.persistence;

import com.marealta.pousada.mail.Channel;
import com.marealta.pousada.mail.Message;
import com.marealta.pousada.mail.MessageStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcMessageRepository implements MessageRepository {
    private final DataSource dataSource;

    public JdbcMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Message m) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?,?,?,?,?)")) {
            s.setString(1, m.channel().name());
            s.setString(2, m.to());
            s.setString(3, m.subject());
            s.setString(4, m.body());
            s.setString(5, m.status().name());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Message> findAll() {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id")) {
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                        MessageStatus.valueOf(rs.getString("status"))));
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
