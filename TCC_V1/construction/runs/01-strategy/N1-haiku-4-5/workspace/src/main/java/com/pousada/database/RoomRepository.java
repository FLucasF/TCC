package com.pousada.database;

import com.pousada.model.Room;
import com.pousada.model.RoomType;
import java.math.BigDecimal;
import java.sql.Connection;
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

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws SQLException {
        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {

            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            new BigDecimal(rs.getString("nightly_rate"))
        );
    }
}
