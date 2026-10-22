package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public static Message create(Message message) throws SQLException {
        String sql = "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, message.getChannel().name());
            stmt.setString(2, message.getTo());
            stmt.setString(3, message.getSubject());
            stmt.setString(4, message.getBody());

            stmt.executeUpdate();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                message.setId(generatedKeys.getLong(1));
            }
        }
        return message;
    }

    public static List<Message> findAll() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            var stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id, channel, to_address, subject, body FROM messages ORDER BY id"
            );
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getLong("id"));
                msg.setChannel(Message.Channel.valueOf(rs.getString("channel")));
                msg.setTo(rs.getString("to_address"));
                msg.setSubject(rs.getString("subject"));
                msg.setBody(rs.getString("body"));
                messages.add(msg);
            }
        }
        return messages;
    }
}
