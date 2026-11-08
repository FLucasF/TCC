package com.pousadamarealta.repository;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoomRepository {
    private static final Map<String, RoomType> ROOMS = Map.of(
        "101", RoomType.STANDARD,
        "102", RoomType.STANDARD,
        "201", RoomType.DELUXE,
        "202", RoomType.DELUXE,
        "301", RoomType.SUITE
    );

    private final Connection connection;

    public RoomRepository(Connection connection) {
        this.connection = connection;
    }

    public void initializeRooms() throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM rooms WHERE number = ?";
        String insertSql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";

        for (Map.Entry<String, RoomType> entry : ROOMS.entrySet()) {
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, entry.getKey());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, entry.getKey());
                            insertStmt.setString(2, entry.getValue().name());
                            insertStmt.setInt(3, entry.getValue().getCapacity());
                            insertStmt.setBigDecimal(4, entry.getValue().getNightlyRate());
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
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

    public Room findByNumber(String number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    );
                }
            }
        }
        return null;
    }

    public static RoomType getRoomType(String number) {
        return ROOMS.get(number);
    }
}
