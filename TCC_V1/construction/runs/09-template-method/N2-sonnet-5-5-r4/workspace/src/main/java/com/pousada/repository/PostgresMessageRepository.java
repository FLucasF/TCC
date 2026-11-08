package com.pousada.repository;

import com.pousada.domain.Channel;
import com.pousada.domain.Message;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PostgresMessageRepository implements MessageRepository {
    private final Database database;

    public PostgresMessageRepository(Database database) {
        this.database = database;
    }

    @Override
    public Message save(Message message) {
        return database.inTransaction(c -> {
            try (var st = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id")) {
                st.setString(1, message.channel().name());
                st.setString(2, message.to());
                st.setString(3, message.subject());
                st.setString(4, message.body());
                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    return new Message(rs.getLong(1), message.channel(), message.to(), message.subject(), message.body());
                }
            }
        });
    }

    @Override
    public List<Message> findAll() {
        return database.inTransaction(c -> {
            var result = new ArrayList<Message>();
            try (var st = c.prepareStatement("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    result.add(new Message(rs.getLong("id"), Channel.valueOf(rs.getString("channel")),
                            rs.getString("recipient"), rs.getString("subject"), rs.getString("body")));
                }
            }
            return result;
        });
    }
}
