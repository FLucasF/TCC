package com.pousadamarealta.repository;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
