package br.com.marealta.repository;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepositoryImpl implements RoomRepository {
    private final DataSource dataSource;

    public RoomRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            if (count(conn) == 0) {
                insert(conn, 101, RoomType.STANDARD);
                insert(conn, 102, RoomType.STANDARD);
                insert(conn, 201, RoomType.DELUXE);
                insert(conn, 202, RoomType.DELUXE);
                insert(conn, 301, RoomType.SUITE);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private int count(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insert(Connection conn, int number, RoomType type) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO rooms (number, type) VALUES (?, ?)")) {
            stmt.setInt(1, number);
            stmt.setString(2, type.name());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Room> findAll() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type FROM rooms ORDER BY number")) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all rooms", e);
        }
    }

    @Override
    public Optional<Room> findByNumber(int number) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT number, type FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number", e);
        }
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("number"),
                RoomType.valueOf(rs.getString("type"))
        );
    }
}
