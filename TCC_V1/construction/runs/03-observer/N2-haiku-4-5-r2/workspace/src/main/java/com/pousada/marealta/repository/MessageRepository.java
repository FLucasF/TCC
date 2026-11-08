package com.pousada.marealta.repository;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.persistence.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
  public static Message save(String channel, String to, String subject, String body) throws SQLException {
    String sql = "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?) RETURNING id";

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, channel);
      pstmt.setString(2, to);
      pstmt.setString(3, subject);
      pstmt.setString(4, body);

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          long id = rs.getLong("id");
          return findById(id);
        }
      }
    }
    throw new SQLException("Failed to save message");
  }

  public static Message findById(long id) throws SQLException {
    String sql = "SELECT id, channel, \"to\", subject, body FROM messages WHERE id = ?";

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setLong(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return new Message(
              rs.getLong("id"),
              rs.getString("channel"),
              rs.getString("to"),
              rs.getString("subject"),
              rs.getString("body")
          );
        }
      }
    }
    return null;
  }

  public static List<Message> findAll() throws SQLException {
    List<Message> messages = new ArrayList<>();
    try (Connection conn = Database.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id")) {
      while (rs.next()) {
        messages.add(new Message(
            rs.getLong("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body")
        ));
      }
    }
    return messages;
  }
}
