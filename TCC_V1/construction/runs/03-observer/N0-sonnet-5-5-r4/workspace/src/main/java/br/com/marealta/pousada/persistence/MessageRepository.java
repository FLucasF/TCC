package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas. */
public final class MessageRepository {
    private final Connection c;

    MessageRepository(Connection c) {
        this.c = c;
    }

    public void add(Message m) {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?)")) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), Message.Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
