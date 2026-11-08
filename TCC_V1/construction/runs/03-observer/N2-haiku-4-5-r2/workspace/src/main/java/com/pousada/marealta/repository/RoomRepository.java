package com.pousada.marealta.repository;

import com.pousada.marealta.domain.Room;
import com.pousada.marealta.persistence.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRepository {
  public static List<Room> findAll() throws SQLException {
    List<Room> rooms = new ArrayList<>();
    try (Connection conn = Database.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
      while (rs.next()) {
        Room room = new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
        rooms.add(room);
      }
    }
    return rooms;
  }

  public static Room findByNumber(int number) throws SQLException {
    try (Connection conn = Database.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number)) {
      if (rs.next()) {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
      }
    }
    return null;
  }

  public static Map<Integer, Room> findAllAsMap() throws SQLException {
    Map<Integer, Room> rooms = new HashMap<>();
    for (Room room : findAll()) {
      rooms.put(room.getNumber(), room);
    }
    return rooms;
  }
}
