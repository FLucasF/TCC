package br.pousada.marealta.database;

import br.pousada.marealta.model.Message;
import br.pousada.marealta.model.MessageChannel;
import br.pousada.marealta.model.MessageStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public static void insert(Connection conn, Message message) throws Exception {
        String sql = """
            INSERT INTO messages (channel, recipient, subject, body, status)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getChannel().toString());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());
            stmt.setString(5, message.getStatus().toString());
            stmt.executeUpdate();
        }
    }

    public static List<Message> findAll() throws Exception {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id")) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body"),
                    MessageStatus.valueOf(rs.getString("status"))
                ));
            }
        }
        return messages;
    }
}
