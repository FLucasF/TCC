package com.pousada.repository;

import com.pousada.infrastructure.Database;
import com.pousada.model.Room;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
  public Optional<Room> findById(int number) {
    String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number;

    try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        return Optional.of(mapRoom(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find room", e);
    }

    return Optional.empty();
  }

  public List<Room> findAll() {
    List<Room> rooms = new ArrayList<>();
    String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

    try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        rooms.add(mapRoom(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find all rooms", e);
    }

    return rooms;
  }

  private Room mapRoom(ResultSet rs) throws SQLException {
    int number = rs.getInt("number");
    String type = rs.getString("type");
    int capacity = rs.getInt("capacity");
    BigDecimal nightlyRate = rs.getBigDecimal("nightly_rate");

    return new Room(number, Room.Type.valueOf(type), capacity, nightlyRate);
  }
}
