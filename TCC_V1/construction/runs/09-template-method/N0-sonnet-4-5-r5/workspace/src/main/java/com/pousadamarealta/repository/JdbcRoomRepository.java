package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoomRepository implements RoomRepository {
    private final DataSource dataSource;

    public JdbcRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                );
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    @Override
    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    );
                    return Optional.of(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }

    @Override
    public void initialize() {
        String checkSql = "SELECT COUNT(*) FROM rooms";
        String insertSql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery(checkSql)) {

            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertRoom(insertStmt, "101", RoomType.STANDARD);
                insertRoom(insertStmt, "102", RoomType.STANDARD);
                insertRoom(insertStmt, "201", RoomType.DELUXE);
                insertRoom(insertStmt, "202", RoomType.DELUXE);
                insertRoom(insertStmt, "301", RoomType.SUITE);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private void insertRoom(PreparedStatement stmt, String number, RoomType type) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type.name());
        stmt.setInt(3, type.getCapacity());
        stmt.setBigDecimal(4, type.getNightlyRate());
        stmt.executeUpdate();
    }
}
