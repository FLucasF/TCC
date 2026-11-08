package pousada.repository;

import pousada.config.Database;
import pousada.domain.Room;
import pousada.domain.RoomType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public void initializeRooms() {
        try (Connection conn = Database.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM rooms";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO rooms (number, type) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                insertRoom(pstmt, "101", RoomType.STANDARD);
                insertRoom(pstmt, "102", RoomType.STANDARD);
                insertRoom(pstmt, "201", RoomType.DELUXE);
                insertRoom(pstmt, "202", RoomType.DELUXE);
                insertRoom(pstmt, "301", RoomType.SUITE);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private void insertRoom(PreparedStatement pstmt, String number, RoomType type) throws SQLException {
        pstmt.setString(1, number);
        pstmt.setString(2, type.name());
        pstmt.executeUpdate();
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, number);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
