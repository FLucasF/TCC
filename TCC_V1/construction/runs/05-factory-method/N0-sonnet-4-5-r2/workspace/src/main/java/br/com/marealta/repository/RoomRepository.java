package br.com.marealta.repository;

import br.com.marealta.model.Room;
import br.com.marealta.model.RoomType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding room", e);
        }

        return Optional.empty();
    }

    public void initializeRooms() {
        String sql = "INSERT INTO rooms (number, type) VALUES (?, ?) ON CONFLICT (number) DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            insertRoom(stmt, "101", RoomType.STANDARD);
            insertRoom(stmt, "102", RoomType.STANDARD);
            insertRoom(stmt, "201", RoomType.DELUXE);
            insertRoom(stmt, "202", RoomType.DELUXE);
            insertRoom(stmt, "301", RoomType.SUITE);

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing rooms", e);
        }
    }

    private void insertRoom(PreparedStatement stmt, String number, RoomType type) throws SQLException {
        stmt.setString(1, number);
        stmt.setString(2, type.name());
        stmt.executeUpdate();
    }
}
