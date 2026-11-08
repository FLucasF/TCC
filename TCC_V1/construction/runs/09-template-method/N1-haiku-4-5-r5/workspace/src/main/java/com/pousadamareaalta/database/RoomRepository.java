package com.pousadamareaalta.database;

import com.pousadamareaalta.domain.Room;
import java.sql.*;
import java.util.*;

public class RoomRepository {
    public static List<Room> findAll() throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Statement stmt = Database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getDouble("nightly_rate")
                ));
            }
        }
        return rooms;
    }

    public static Optional<Room> findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (PreparedStatement stmt = Database.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getDouble("nightly_rate")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
