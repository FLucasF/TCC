package com.posadamarealta.repository;

import com.posadamarealta.model.Room;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DataSource ds;

    public RoomRepository(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = ds.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
