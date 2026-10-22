package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final RoomRepository instance = new RoomRepository();

    private RoomRepository() {
    }

    public static RoomRepository getInstance() {
        return instance;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getInstance().getConnection()) {
            var rs = conn.createStatement().executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number"
            );
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = Database.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?"
            );
            ps.setInt(1, number);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
        return Optional.empty();
    }

    public void saveAll(List<Room> rooms) {
        try (Connection conn = Database.getInstance().getConnection()) {
            for (Room room : rooms) {
                var ps = conn.prepareStatement(
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)" +
                    " ON CONFLICT (number) DO UPDATE SET type = ?, capacity = ?, nightly_rate = ?"
                );
                ps.setInt(1, room.number());
                ps.setString(2, room.type().name());
                ps.setInt(3, room.capacity());
                ps.setBigDecimal(4, room.nightlyRate());
                ps.setString(5, room.type().name());
                ps.setInt(6, room.capacity());
                ps.setBigDecimal(7, room.nightlyRate());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save rooms", e);
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
