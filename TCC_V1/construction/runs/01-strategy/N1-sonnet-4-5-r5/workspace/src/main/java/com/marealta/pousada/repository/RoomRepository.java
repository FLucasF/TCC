package com.marealta.pousada.repository;

import com.marealta.pousada.domain.model.Room;
import com.marealta.pousada.domain.model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    private final Connection connection;

    public RoomRepository(Connection connection) {
        this.connection = connection;
    }

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public void initializeRooms() throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM rooms";
        try (PreparedStatement stmt = connection.prepareStatement(checkSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO rooms (number, type) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setInt(1, 101);
            stmt.setString(2, "STANDARD");
            stmt.addBatch();

            stmt.setInt(1, 102);
            stmt.setString(2, "STANDARD");
            stmt.addBatch();

            stmt.setInt(1, 201);
            stmt.setString(2, "DELUXE");
            stmt.addBatch();

            stmt.setInt(1, 202);
            stmt.setString(2, "DELUXE");
            stmt.addBatch();

            stmt.setInt(1, 301);
            stmt.setString(2, "SUITE");
            stmt.addBatch();

            stmt.executeBatch();
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        RoomType.valueOf(rs.getString("type"))
                    ));
                }
            }
        }

        return Optional.empty();
    }
}
