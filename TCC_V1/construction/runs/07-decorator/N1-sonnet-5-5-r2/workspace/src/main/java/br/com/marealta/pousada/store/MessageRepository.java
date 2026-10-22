package br.com.marealta.pousada.store;

import br.com.marealta.pousada.mail.Channel;
import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.mail.MessageStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void insert(Connection c, Message m) throws SQLException {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, m.channel().name());
            s.setString(2, m.to());
            s.setString(3, m.subject());
            s.setString(4, m.body());
            s.setString(5, m.status().name());
            s.executeUpdate();
        }
    }

    public List<Message> findAll(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM messages ORDER BY id");
             ResultSet rs = s.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body"),
                        MessageStatus.valueOf(rs.getString("status"))));
            }
            return messages;
        }
    }
}
