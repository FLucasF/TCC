package com.marealta.infrastructure.database;

import com.marealta.domain.model.Room;
import com.marealta.domain.model.RoomType;

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

    public void initializeRooms() throws SQLException {
        String sql = """
            INSERT INTO rooms (number, type) VALUES
            ('101', 'STANDARD'),
            ('102', 'STANDARD'),
            ('201', 'DELUXE'),
            ('202', 'DELUXE'),
            ('301', 'SUITE')
            ON CONFLICT (number) DO NOTHING
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
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
                    return Optional.of(new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    ));
                }
            }
        }

        return Optional.empty();
    }
}
