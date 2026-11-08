package com.marealta.db;

import com.marealta.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    Room.RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            logger.error("Error finding all rooms", e);
            throw new RuntimeException(e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(int roomNumber) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room(
                        rs.getInt("number"),
                        Room.RoomType.valueOf(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    );
                    return Optional.of(room);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding room by number", e);
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }
}
