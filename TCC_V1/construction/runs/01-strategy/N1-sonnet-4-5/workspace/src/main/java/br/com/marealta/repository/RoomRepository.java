package br.com.marealta.repository;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initSchema() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number VARCHAR(10) PRIMARY KEY,
                type VARCHAR(20) NOT NULL
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create rooms table", e);
        }
    }

    public void initData() {
        try (Connection conn = dataSource.getConnection()) {
            String check = "SELECT COUNT(*) FROM rooms";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(check)) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }

            String insert = "INSERT INTO rooms (number, type) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                insertRoom(stmt, "101", RoomType.STANDARD);
                insertRoom(stmt, "102", RoomType.STANDARD);
                insertRoom(stmt, "201", RoomType.DELUXE);
                insertRoom(stmt, "202", RoomType.DELUXE);
                insertRoom(stmt, "301", RoomType.SUITE);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize room data", e);
        }
    }

    private void insertRoom(PreparedStatement stmt, String number, RoomType type) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type.name());
        stmt.executeUpdate();
    }

    public List<Room> findAll() {
        String query = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String query = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
