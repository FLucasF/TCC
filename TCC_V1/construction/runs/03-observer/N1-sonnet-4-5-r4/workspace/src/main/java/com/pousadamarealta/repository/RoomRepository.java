package com.pousadamarealta.repository;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Connection connection;

    public RoomRepository(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }

        if (findAll().isEmpty()) {
            insert(new Room("101", RoomType.STANDARD));
            insert(new Room("102", RoomType.STANDARD));
            insert(new Room("201", RoomType.DELUXE));
            insert(new Room("202", RoomType.DELUXE));
            insert(new Room("301", RoomType.SUITE));
        }
    }

    private void insert(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (number, type) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.executeUpdate();
        }
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
