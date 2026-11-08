package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.Message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public Message save(Message message) {
        String sql = "INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id";

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getChannel());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            var rs = stmt.executeQuery();
            rs.next();
            message.setId(rs.getLong(1));
            return message;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar mensagem", e);
        }
    }

    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {

            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar mensagens", e);
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setChannel(rs.getString("channel"));
        m.setTo(rs.getString("recipient"));
        m.setSubject(rs.getString("subject"));
        m.setBody(rs.getString("body"));
        return m;
    }
}
