package com.pousada.database;

import com.pousada.model.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
  public static void save(String channel, String to, String subject, String body) {
    String sql =
        "INSERT INTO messages (channel, to_addr, subject, body) VALUES (?, ?, ?, ?)";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, channel);
      stmt.setString(2, to);
      stmt.setString(3, subject);
      stmt.setString(4, body);
      stmt.executeUpdate();
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Message> findAll() {
    List<Message> messages = new ArrayList<>();
    String sql = "SELECT id, channel, to_addr, subject, body FROM messages ORDER BY id";

    try (Connection conn = Database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        messages.add(
            new Message(
                rs.getLong("id"),
                rs.getString("channel"),
                rs.getString("to_addr"),
                rs.getString("subject"),
                rs.getString("body")));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return messages;
  }
}
