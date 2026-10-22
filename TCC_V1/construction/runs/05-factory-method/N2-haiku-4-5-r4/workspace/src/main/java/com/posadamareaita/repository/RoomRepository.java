package com.posadamareaita.repository;

import com.posadamareaita.database.Database;
import com.posadamareaita.domain.Room;
import com.posadamareaita.domain.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public static Optional<Room> findByNumber(int number) throws SQLException {
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
            stmt.setInt(1, number);
            var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRoom(resultSet));
            }
            return Optional.empty();
        }
    }

    public static List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             var stmt = conn.createStatement()) {
            var resultSet = stmt.executeQuery("SELECT * FROM rooms ORDER BY number");
            while (resultSet.next()) {
                rooms.add(mapRoom(resultSet));
            }
        }
        return rooms;
    }

    private static Room mapRoom(ResultSet rs) throws SQLException {
        int number = rs.getInt("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        int capacity = rs.getInt("capacity");
        BigDecimal nightlyRate = rs.getBigDecimal("nightly_rate");
        return new Room(number, type, capacity, nightlyRate);
    }
}
