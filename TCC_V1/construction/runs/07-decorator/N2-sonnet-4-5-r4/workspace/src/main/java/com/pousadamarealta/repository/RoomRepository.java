package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initializeRooms() {
        String sql = """
            INSERT INTO rooms (number, type) VALUES
            ('101', 'STANDARD'),
            ('102', 'STANDARD'),
            ('201', 'DELUXE'),
            ('202', 'DELUXE'),
            ('301', 'SUITE')
            ON CONFLICT (number) DO NOTHING
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find rooms", e);
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
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }
}
