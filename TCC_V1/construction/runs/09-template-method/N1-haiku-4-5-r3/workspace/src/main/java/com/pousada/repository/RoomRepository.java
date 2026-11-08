package com.pousada.repository;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Room;
import com.pousada.model.RoomType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    public static void initializeRooms() {
        String sql = """
            INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
            (101, 'STANDARD', 2, 249.90),
            (102, 'STANDARD', 2, 249.90),
            (201, 'DELUXE', 3, 389.90),
            (202, 'DELUXE', 3, 389.90),
            (301, 'SUITE', 4, 579.90)
            ON CONFLICT DO NOTHING;
            """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    public static List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int number = rs.getInt("number");
                String typeStr = rs.getString("type");
                RoomType type = RoomType.fromString(typeStr);
                rooms.add(new Room(number, type));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public static Room findByNumber(int number) {
        String sql = "SELECT number, type FROM rooms WHERE number = " + number;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String typeStr = rs.getString("type");
                RoomType type = RoomType.fromString(typeStr);
                return new Room(number, type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return null;
    }
}
