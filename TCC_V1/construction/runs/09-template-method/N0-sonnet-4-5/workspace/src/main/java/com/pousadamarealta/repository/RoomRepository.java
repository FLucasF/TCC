package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT number, type FROM rooms ORDER BY number")) {

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
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            try (var rs = stmt.executeQuery()) {
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
