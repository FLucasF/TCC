package com.pousada.repository;

import com.pousada.config.Database;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
        initializeRooms();
    }

    private void initializeRooms() {
        try (Connection conn = database.getConnection()) {
            if (count(conn) == 0) {
                insert(conn, new Room("101", RoomType.STANDARD));
                insert(conn, new Room("102", RoomType.STANDARD));
                insert(conn, new Room("201", RoomType.DELUXE));
                insert(conn, new Room("202", RoomType.DELUXE));
                insert(conn, new Room("301", RoomType.SUITE));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private long count(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insert(Connection conn, Room room) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getNightlyRate());
            stmt.executeUpdate();
        }
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rooms.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number", e);
        }

        return Optional.empty();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
