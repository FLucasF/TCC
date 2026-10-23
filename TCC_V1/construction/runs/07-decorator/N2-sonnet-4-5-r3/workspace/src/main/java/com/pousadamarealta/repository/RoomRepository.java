package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {
    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Room> findAll() {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {

            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }
    }

    public Optional<Room> findByNumber(String number) {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }
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
