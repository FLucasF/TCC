package com.pousada.repo;

import com.pousada.domain.MessageChannel;
import com.pousada.domain.SentMessage;
import com.pousada.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    public void send(MessageChannel channel, String to, String subject, String body) throws Exception {
        Connection conn = Database.getConnection();

        String sql = "INSERT INTO sent_messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel.name());
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.executeUpdate();
        }
    }

    public List<SentMessage> findAll() throws Exception {
        Connection conn = Database.getConnection();
        List<SentMessage> messages = new ArrayList<>();

        String sql = "SELECT id, channel, to_address, subject, body FROM sent_messages ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(new SentMessage(
                    rs.getInt("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }

        return messages;
    }
}
