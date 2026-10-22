package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRoomRepository implements RoomRepository {
    private final DataSource dataSource;

    public JdbcRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    @Override
    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getString("number"),
                        RoomType.valueOf(rs.getString("type"))
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
