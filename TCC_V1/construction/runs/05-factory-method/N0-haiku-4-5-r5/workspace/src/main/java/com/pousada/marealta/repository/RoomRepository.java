package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            var stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    Room.Type.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
                rooms.add(room);
            }
        }
        return rooms;
    }

    public static Room findByNumber(int number) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?");
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    Room.Type.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        }
        return null;
    }
}
