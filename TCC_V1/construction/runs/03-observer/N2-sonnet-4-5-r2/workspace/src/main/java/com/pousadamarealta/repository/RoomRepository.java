package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initializeRooms() {
        String sql = "INSERT INTO rooms (number, type) VALUES (?, ?) ON CONFLICT (number) DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            addRoom(stmt, "101", RoomType.STANDARD);
            addRoom(stmt, "102", RoomType.STANDARD);
            addRoom(stmt, "201", RoomType.DELUXE);
            addRoom(stmt, "202", RoomType.DELUXE);
            addRoom(stmt, "301", RoomType.SUITE);

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing rooms", e);
        }
    }

    private void addRoom(PreparedStatement stmt, String number, RoomType type) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type.name());
        stmt.executeUpdate();
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Room room = new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    );
                    return Optional.of(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding room", e);
        }

        return Optional.empty();
    }
}
