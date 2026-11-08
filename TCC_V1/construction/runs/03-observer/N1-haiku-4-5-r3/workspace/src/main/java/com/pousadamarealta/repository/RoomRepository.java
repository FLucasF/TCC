package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public List<Room> findAll() throws SQLException {
        String sql = "SELECT * FROM rooms ORDER BY number";

        List<Room> rooms = new ArrayList<>();
        try (Statement stmt = Database.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE number = ?";

        java.sql.PreparedStatement stmt = Database.getConnection().prepareStatement(sql);
        stmt.setInt(1, number);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }

        return Optional.empty();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("number"),
                rs.getString("type"),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate")
        );
    }
}
