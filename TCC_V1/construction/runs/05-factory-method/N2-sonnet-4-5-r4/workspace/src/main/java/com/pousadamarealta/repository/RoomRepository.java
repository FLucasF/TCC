package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS rooms (" +
                "number INTEGER PRIMARY KEY, " +
                "type VARCHAR(20) NOT NULL)"
            );

            if (count() == 0) {
                insertRoom(new Room(101, RoomType.STANDARD));
                insertRoom(new Room(102, RoomType.STANDARD));
                insertRoom(new Room(201, RoomType.DELUXE));
                insertRoom(new Room(202, RoomType.DELUXE));
                insertRoom(new Room(301, RoomType.SUITE));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms table", e);
        }
    }

    private int count() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insertRoom(Room room) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO rooms (number, type) VALUES (?, ?)")) {
            stmt.setInt(1, room.number());
            stmt.setString(2, room.type().name());
            stmt.executeUpdate();
        }
    }

    public List<Room> findAll() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number");
             ResultSet rs = stmt.executeQuery()) {

            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        RoomType.valueOf(rs.getString("type"))
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }
    }
}
