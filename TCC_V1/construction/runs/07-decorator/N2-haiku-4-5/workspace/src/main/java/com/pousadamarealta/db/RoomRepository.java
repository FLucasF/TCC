package com.pousadamarealta.db;

import com.pousadamarealta.model.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
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

    public Optional<Room> findByNumber(Integer number) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
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
