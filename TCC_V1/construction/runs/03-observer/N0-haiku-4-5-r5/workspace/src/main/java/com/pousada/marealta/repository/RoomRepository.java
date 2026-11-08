package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Room;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    Room.RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }

        return rooms;
    }

    public Room findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (var conn = DatabaseConfig.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                        rs.getInt("number"),
                        Room.RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    );
                }
            }
        }

        return null;
    }
}
