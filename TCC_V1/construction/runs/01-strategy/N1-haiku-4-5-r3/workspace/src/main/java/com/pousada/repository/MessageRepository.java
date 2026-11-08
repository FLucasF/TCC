package com.pousada.repository;

import com.pousada.infrastructure.Database;
import com.pousada.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
  public Message save(Message message) {
    String sql = """
        INSERT INTO messages (channel, "to", subject, body)
        VALUES (?, ?, ?, ?)
        """;

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, message.getChannel().toString());
      stmt.setString(2, message.getTo());
      stmt.setString(3, message.getSubject());
      stmt.setString(4, message.getBody());

      stmt.executeUpdate();

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          message.setId(generatedKeys.getLong(1));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save message", e);
    }

    return message;
  }

  public List<Message> findAll() {
    List<Message> messages = new ArrayList<>();
    String sql = """
        SELECT id, channel, "to", subject, body FROM messages ORDER BY id
        """;

    try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        messages.add(mapMessage(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find all messages", e);
    }

    return messages;
  }

  private Message mapMessage(ResultSet rs) throws SQLException {
    Message message =
        new Message(
            Message.Channel.valueOf(rs.getString("channel")),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body"));
    message.setId(rs.getLong("id"));
    return message;
  }
}
