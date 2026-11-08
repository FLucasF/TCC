package com.pousadamarealta.db;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }
        return rooms;
    }

    public static Optional<Room> findByNumber(int number) throws SQLException {
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
