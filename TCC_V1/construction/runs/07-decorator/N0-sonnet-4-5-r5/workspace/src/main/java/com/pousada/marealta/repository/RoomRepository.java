package com.pousada.marealta.repository;

import com.pousada.marealta.config.Database;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.domain.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching rooms", e);
        }

        return rooms;
    }

    public Room findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching room", e);
        }

        return null;
    }
}
