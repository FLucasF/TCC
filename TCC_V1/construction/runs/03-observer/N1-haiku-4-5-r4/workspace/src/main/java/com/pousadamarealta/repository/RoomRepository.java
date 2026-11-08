package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.models.Room;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {
            while (rs.next()) {
                rooms.add(mapToRoom(rs));
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE number = " + number)) {
            if (rs.next()) {
                return Optional.of(mapToRoom(rs));
            }
        }
        return Optional.empty();
    }

    private Room mapToRoom(ResultSet rs) throws Exception {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
