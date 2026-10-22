package com.pousada.repository;

import com.pousada.db.Database;
import com.pousada.domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private static final Logger log = LoggerFactory.getLogger(RoomRepository.class);

    public Room findById(int number) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?"
            );
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRoom(rs);
            }
            return null;
        }
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number"
            );

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }
        return rooms;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            Room.Type.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
