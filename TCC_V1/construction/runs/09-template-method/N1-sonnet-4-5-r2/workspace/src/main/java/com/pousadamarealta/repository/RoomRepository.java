package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DatabaseManager dbManager;

    public RoomRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initializeRooms();
    }

    private void initializeRooms() {
        try (Connection conn = dbManager.getConnection()) {
            if (isEmpty(conn)) {
                insertRoom(conn, 101, RoomType.STANDARD);
                insertRoom(conn, 102, RoomType.STANDARD);
                insertRoom(conn, 201, RoomType.DELUXE);
                insertRoom(conn, 202, RoomType.DELUXE);
                insertRoom(conn, 301, RoomType.SUITE);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private boolean isEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    private void insertRoom(Connection conn, int number, RoomType type) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            stmt.setString(2, type.name());
            stmt.setInt(3, type.getCapacity());
            stmt.setBigDecimal(4, type.getNightlyRate());
            stmt.executeUpdate();
        }
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int number = rs.getInt("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
