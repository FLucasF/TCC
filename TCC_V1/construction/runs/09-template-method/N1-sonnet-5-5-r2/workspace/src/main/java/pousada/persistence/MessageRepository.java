package pousada.persistence;

import pousada.domain.Message;
import pousada.domain.MessageChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** A lista de mensagens enviadas. */
public class MessageRepository {
    private final Connection connection;

    MessageRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";
        try (PreparedStatement st = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {
            st.setString(1, message.channel().name());
            st.setString(2, message.to());
            st.setString(3, message.subject());
            st.setString(4, message.body());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    public List<Message> all() {
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = st.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), MessageChannel.valueOf(rs.getString("channel")),
                        rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }
}
