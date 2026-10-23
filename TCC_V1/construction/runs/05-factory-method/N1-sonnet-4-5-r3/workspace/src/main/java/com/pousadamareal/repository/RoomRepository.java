package com.pousadamareal.repository;

import com.pousadamareal.model.Room;
import com.pousadamareal.model.RoomType;

import java.sql.Connection;
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
        try (Connection conn = database.getConnection();
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
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
        return Optional.empty();
    }
}
