package com.pousadamareaita.service;

import com.pousadamareaita.database.Database;
import com.pousadamareaita.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final Database db;

    public MessageService(Database db) {
        this.db = db;
    }

    public void sendMessage(String channel, String recipient, String subject, String body) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String sql = """
                INSERT INTO messages (channel, recipient, subject, body)
                VALUES (?, ?, ?, ?)
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, channel);
                pstmt.setString(2, recipient);
                pstmt.setString(3, subject);
                pstmt.setString(4, body);
                pstmt.executeUpdate();
            }
        }
    }

    public List<Message> getMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String sql = """
                SELECT id, channel, recipient, subject, body
                FROM messages
                ORDER BY created_at, id
            """;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    messages.add(new Message(
                        rs.getLong("id"),
                        rs.getString("channel"),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("body")
                    ));
                }
            }
        }
        return messages;
    }
}
