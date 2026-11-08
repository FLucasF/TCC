package com.pousada.marealta.persistence;

import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDao {
    private final DataSource dataSource;

    public RoomDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (number) DO UPDATE SET type = ?, capacity = ?, nightly_rate = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, room.getNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getNightlyRate());
            stmt.setString(5, room.getType().name());
            stmt.setInt(6, room.getCapacity());
            stmt.setBigDecimal(7, room.getNightlyRate());
            stmt.executeUpdate();
        }
    }

    public Room findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
            return null;
        }
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }
        return rooms;
    }
}
