package com.pousadamarealta.database;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {
    private static final RoomDAO INSTANCE = new RoomDAO();

    private RoomDAO() {
    }

    public static RoomDAO getInstance() {
        return INSTANCE;
    }

    public void initialize() throws SQLException {
        List<Room> rooms = getAllRooms();
        if (rooms.isEmpty()) {
            insertDefaultRooms();
        }
    }

    private void insertDefaultRooms() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Room[] rooms = {
                new Room(101, RoomType.STANDARD),
                new Room(102, RoomType.STANDARD),
                new Room(201, RoomType.DELUXE),
                new Room(202, RoomType.DELUXE),
                new Room(301, RoomType.SUITE)
            };
            for (Room room : rooms) {
                insertRoom(conn, room);
            }
        }
    }

    private void insertRoom(Connection conn, Room room) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, room.getNumber());
            stmt.setString(2, room.getTypeCode());
            stmt.setInt(3, room.getCapacity());
            stmt.setDouble(4, room.getNightlyRate());
            stmt.executeUpdate();
        }
    }

    public Room getRoomByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRoom(rs);
                }
            }
        }
        return null;
    }

    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }
        return rooms;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        int number = rs.getInt("number");
        String type = rs.getString("type");
        return new Room(number, RoomType.fromCode(type));
    }
}
