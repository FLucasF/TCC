package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
  public static Message insert(
      MessageChannel channel, String to, String subject, String body) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "INSERT INTO messages (channel, to_address, subject, body) VALUES (?, ?, ?, ?) RETURNING id",
              Statement.RETURN_GENERATED_KEYS);
      stmt.setString(1, channel.value);
      stmt.setString(2, to);
      stmt.setString(3, subject);
      stmt.setString(4, body);

      var rs = stmt.executeQuery();
      rs.next();
      long id = rs.getLong(1);

      return new Message(id, channel, to, subject, body);
    }
  }

  public static List<Message> findAll() throws SQLException {
    List<Message> messages = new ArrayList<>();
    try (Connection conn = Database.getConnection()) {
      var rs =
          conn.createStatement()
              .executeQuery("SELECT id, channel, to_address, subject, body FROM messages ORDER BY id");
      while (rs.next()) {
        messages.add(
            new Message(
                rs.getLong("id"),
                MessageChannel.from(rs.getString("channel")),
                rs.getString("to_address"),
                rs.getString("subject"),
                rs.getString("body")));
      }
    }
    return messages;
  }
}
