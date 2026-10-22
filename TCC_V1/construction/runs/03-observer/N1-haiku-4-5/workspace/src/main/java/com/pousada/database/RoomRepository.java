package com.pousada.database;

import com.pousada.model.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
  public static Optional<Room> findByNumber(int number) {
    String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, number);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new Room(
                  rs.getInt("number"),
                  rs.getString("type"),
                  rs.getInt("capacity"),
                  rs.getBigDecimal("nightly_rate")));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return Optional.empty();
  }

  public static List<Room> findAll() {
    List<Room> rooms = new ArrayList<>();
    String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
    try (Connection conn = Database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        rooms.add(
            new Room(
                rs.getInt("number"),
                rs.getString("type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate")));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return rooms;
  }
}
