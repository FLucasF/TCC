package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.notification.Message;
import br.com.pousadamarealta.reservas.notification.MessageGateway;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas, guardada no banco. Grava na transação em andamento. */
public class MessageRepository implements MessageGateway {
    private static final String EMAIL = "EMAIL";

    private final Database db;

    public MessageRepository(Database db) {
        this.db = db;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, EMAIL);
            ps.setString(2, to);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public List<Message> findAll() {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
