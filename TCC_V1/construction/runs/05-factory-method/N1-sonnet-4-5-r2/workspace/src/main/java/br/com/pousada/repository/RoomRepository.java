package br.com.pousada.repository;

import br.com.pousada.domain.Room;
import br.com.pousada.domain.RoomType;

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
        initializeRooms();
    }

    private void initializeRooms() {
        try (Connection conn = dataSource.getConnection()) {
            String createTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    number VARCHAR(10) PRIMARY KEY,
                    type VARCHAR(20) NOT NULL
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createTable)) {
                stmt.execute();
            }

            String count = "SELECT COUNT(*) FROM rooms";
            try (PreparedStatement stmt = conn.prepareStatement(count);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertInitialRooms(conn);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rooms", e);
        }
    }

    private void insertInitialRooms(Connection conn) throws SQLException {
        String insert = "INSERT INTO rooms (number, type) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setString(1, "101");
            stmt.setString(2, "STANDARD");
            stmt.execute();

            stmt.setString(1, "102");
            stmt.setString(2, "STANDARD");
            stmt.execute();

            stmt.setString(1, "201");
            stmt.setString(2, "DELUXE");
            stmt.execute();

            stmt.setString(1, "202");
            stmt.setString(2, "DELUXE");
            stmt.execute();

            stmt.setString(1, "301");
            stmt.setString(2, "SUITE");
            stmt.execute();
        }
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
