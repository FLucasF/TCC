package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

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

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
            }
        }

        return Optional.empty();
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
