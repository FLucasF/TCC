package com.pousada.marealta.infrastructure;

import com.pousada.marealta.domain.Room;
import com.pousada.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE number = " + number)) {
            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
        }
        return Optional.empty();
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
