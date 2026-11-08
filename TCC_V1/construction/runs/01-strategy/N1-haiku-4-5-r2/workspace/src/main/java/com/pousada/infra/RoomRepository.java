package com.pousada.infra;

import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             var rs = stmt.executeQuery()) {

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
        try (var conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {

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
