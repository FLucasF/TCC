package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
  public static Optional<Room> findById(int number) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?");
      stmt.setInt(1, number);
      var rs = stmt.executeQuery();
      if (rs.next()) {
        return Optional.of(
            new Room(
                rs.getInt("number"),
                RoomType.from(rs.getString("type")),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate")));
      }
      return Optional.empty();
    }
  }

  public static List<Room> findAll() throws SQLException {
    List<Room> rooms = new ArrayList<>();
    try (Connection conn = Database.getConnection()) {
      var rs =
          conn.createStatement()
              .executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
      while (rs.next()) {
        rooms.add(
            new Room(
                rs.getInt("number"),
                RoomType.from(rs.getString("type")),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate")));
      }
    }
    return rooms;
  }
}
