package br.com.marealta.repository;

import br.com.marealta.database.Database;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
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
