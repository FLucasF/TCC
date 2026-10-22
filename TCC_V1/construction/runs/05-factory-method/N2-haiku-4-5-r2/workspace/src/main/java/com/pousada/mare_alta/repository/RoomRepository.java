package com.pousada.mare_alta.repository;

import com.pousada.mare_alta.database.Database;
import com.pousada.mare_alta.model.Room;
import com.pousada.mare_alta.model.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

    public Optional<Room> findById(int number) throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToRoom(rs));
            }
            return Optional.empty();
        }
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM rooms ORDER BY number")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }
        }
        return rooms;
    }

    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("number"),
                RoomType.fromValue(rs.getString("type")),
                rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate")
        );
    }
}
