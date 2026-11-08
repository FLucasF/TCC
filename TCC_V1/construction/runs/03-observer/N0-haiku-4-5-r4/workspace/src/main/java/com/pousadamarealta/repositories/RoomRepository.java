package com.pousadamarealta.repositories;

import com.pousadamarealta.database.DatabaseConfig;
import com.pousadamarealta.models.Room;
import com.pousadamarealta.models.RoomType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class RoomRepository {
    private final DatabaseConfig dbConfig;

    public RoomRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = dbConfig.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
                    while (rs.next()) {
                        rooms.add(new Room(
                            rs.getInt("number"),
                            RoomType.valueOf(rs.getString("type")),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("nightly_rate")
                        ));
                    }
                }
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
                stmt.setInt(1, number);
                try (ResultSet rs = stmt.executeQuery()) {
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
        }
        return Optional.empty();
    }
}
