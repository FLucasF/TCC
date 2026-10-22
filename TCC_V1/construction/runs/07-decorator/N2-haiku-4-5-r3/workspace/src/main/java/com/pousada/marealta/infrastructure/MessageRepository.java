package com.pousada.marealta.infrastructure;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final Database database;

    public MessageRepository(Database database) {
        this.database = database;
    }

    public void save(Message message) throws SQLException {
        String sql = """
                INSERT INTO messages (channel, "to", subject, body, status)
                VALUES ('%s', '%s', '%s', '%s', '%s')
                """.formatted(
                escape(message.getChannel()),
                escape(message.getTo()),
                escape(message.getSubject()),
                escape(message.getBody()),
                message.getStatus().name());

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public void saveAll(List<Message> messages) throws SQLException {
        for (Message message : messages) {
            save(message);
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setChannel(rs.getString("channel"));
        m.setTo(rs.getString("to"));
        m.setSubject(rs.getString("subject"));
        m.setBody(rs.getString("body"));
        m.setStatus(MessageStatus.valueOf(rs.getString("status")));
        return m;
    }

    private String escape(String value) {
        return value.replace("'", "''");
    }
}
