package br.com.marealto.repository;

import br.com.marealto.model.Room;
import br.com.marealto.model.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public void initializeRooms() {
        try (Connection conn = Database.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM rooms";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                insertRoom(stmt, "101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
                insertRoom(stmt, "102", RoomType.STANDARD, 2, new BigDecimal("249.90"));
                insertRoom(stmt, "201", RoomType.DELUXE, 3, new BigDecimal("389.90"));
                insertRoom(stmt, "202", RoomType.DELUXE, 3, new BigDecimal("389.90"));
                insertRoom(stmt, "301", RoomType.SUITE, 4, new BigDecimal("579.90"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private void insertRoom(PreparedStatement stmt, String number, RoomType type, int capacity, BigDecimal rate) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type.name());
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, rate);
        stmt.executeUpdate();
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getString("number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
