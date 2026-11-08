package com.pousada.marealta.infrastructure.persistence.postgres;

import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.domain.room.Room;
import com.pousada.marealta.domain.room.RoomType;
import com.pousada.marealta.domain.shared.Money;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresRoomRepository implements RoomRepository {
    private final DataSource dataSource;

    public PostgresRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT number, type, capacity, nightly_rate_cents FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(mapRowToRoom(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying rooms", e);
        }

        return rooms;
    }

    @Override
    public Optional<Room> findByNumber(int number) {
        String sql = "SELECT number, type, capacity, nightly_rate_cents FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRoom(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying room " + number, e);
        }

        return Optional.empty();
    }

    @Override
    public void save(Room room) {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate_cents) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(number) DO UPDATE SET " +
                "type = EXCLUDED.type, capacity = EXCLUDED.capacity, nightly_rate_cents = EXCLUDED.nightly_rate_cents";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, room.number());
            stmt.setString(2, room.type().displayName());
            stmt.setInt(3, room.capacity());
            stmt.setLong(4, room.nightlyRate().cents());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving room " + room.number(), e);
        }
    }

    private Room mapRowToRoom(ResultSet rs) throws SQLException {
        int number = rs.getInt("number");
        RoomType type = RoomType.fromString(rs.getString("type"));
        Money nightlyRate = Money.ofCents(rs.getLong("nightly_rate_cents"));
        return new Room(number, type, nightlyRate);
    }
}
