package com.pousadamarealta.database;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO {
    private final DatabaseManager dbManager;

    public RoomDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dbManager.getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, number);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
