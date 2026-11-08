package com.pousadamarealta.repository;

import com.pousadamarealta.database.DatabaseManager;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DatabaseManager dbManager;

    public RoomRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (var conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT number, type FROM rooms ORDER BY number")) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        try (var conn = dbManager.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
    }
}
