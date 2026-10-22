package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.RoomNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching rooms", e);
        }

        return rooms;
    }

    public Room findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching room", e);
        }

        throw new RoomNotFoundException("Room not found: " + number);
    }

    public void initializeRooms() {
        String checkSql = "SELECT COUNT(*) FROM rooms";
        String insertSql = "INSERT INTO rooms (number, type) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 ResultSet rs = checkStmt.executeQuery()) {

                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            List<Object[]> rooms = List.of(
                new Object[]{"101", "STANDARD"},
                new Object[]{"102", "STANDARD"},
                new Object[]{"201", "DELUXE"},
                new Object[]{"202", "DELUXE"},
                new Object[]{"301", "SUITE"}
            );

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Object[] room : rooms) {
                    insertStmt.setString(1, (String) room[0]);
                    insertStmt.setString(2, (String) room[1]);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing rooms", e);
        }
    }
}
