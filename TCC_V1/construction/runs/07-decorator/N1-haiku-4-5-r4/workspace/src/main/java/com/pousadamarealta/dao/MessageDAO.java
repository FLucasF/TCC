package com.pousadamarealta.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.pousadamarealta.models.Message;
import com.pousadamarealta.models.MessageChannel;
import com.pousadamarealta.models.MessageStatus;
import com.pousadamarealta.db.Database;

public class MessageDAO {
    public static Message create(MessageChannel channel, String to, String subject, String body, MessageStatus status) throws SQLException {
        String sql = "INSERT INTO messages (channel, recipient, subject, body, status) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.setString(5, status.name());

            ResultSet rs = stmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");

            return getById(id);
        }
    }

    public static Message getById(int id) throws SQLException {
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    public static List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapResultSet(rs));
            }
        }
        return messages;
    }

    private static Message mapResultSet(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            MessageChannel.valueOf(rs.getString("channel")),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body"),
            MessageStatus.valueOf(rs.getString("status"))
        );
    }
}
