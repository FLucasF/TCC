package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.notification.Channel;
import br.com.marealta.pousada.notification.Message;
import br.com.marealta.pousada.notification.MessageSender;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas; também é por onde as mensagens "saem". */
public class MessageRepository implements MessageSender {

    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    @Override
    public void send(Channel channel, String to, String subject, String body) {
        try (PreparedStatement st = db.connection().prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            st.setString(1, channel.name());
            st.setString(2, to);
            st.setString(3, subject);
            st.setString(4, body);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Message> findAll() {
        try (PreparedStatement st = db.connection().prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = st.executeQuery()) {
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
