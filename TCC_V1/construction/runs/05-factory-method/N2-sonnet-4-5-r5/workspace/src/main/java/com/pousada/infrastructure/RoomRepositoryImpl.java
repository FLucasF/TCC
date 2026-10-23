package com.pousada.infrastructure;

import com.pousada.domain.model.Room;
import com.pousada.domain.model.RoomType;
import com.pousada.domain.repository.RoomRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepositoryImpl implements RoomRepository {
    private final DataSource dataSource;

    public RoomRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }

        return rooms;
    }

    @Override
    public Optional<Room> findByNumber(String number) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }

        return Optional.empty();
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
