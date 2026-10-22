package br.com.pousadamareal.repository;

import br.com.pousadamareal.db.DatabaseConnection;
import br.com.pousadamareal.domain.Channel;
import br.com.pousadamareal.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public void save(Message message) throws SQLException {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, message.getChannel().name());
                stmt.setString(2, message.getTo());
                stmt.setString(3, message.getSubject());
                stmt.setString(4, message.getBody());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setId(rs.getLong(1));
                    }
                }
            }
        }
    }

    public List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getLong("id"));
                    m.setChannel(Channel.valueOf(rs.getString("channel")));
                    m.setTo(rs.getString("recipient"));
                    m.setSubject(rs.getString("subject"));
                    m.setBody(rs.getString("body"));
                    messages.add(m);
                }
            }
        }
        return messages;
    }
}
