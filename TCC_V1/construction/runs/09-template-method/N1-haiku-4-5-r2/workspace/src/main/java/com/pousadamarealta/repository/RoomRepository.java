package com.pousadamarealta.repository;

import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {

            while (rs.next()) {
                rooms.add(mapResultSet(rs));
            }
        }
        return rooms;
    }

    public Room findByNumber(int number) throws Exception {
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE number = " + number)) {

            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        return null;
    }

    private Room mapResultSet(ResultSet rs) throws Exception {
        int number = rs.getInt("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        int capacity = rs.getInt("capacity");
        var nightlyRate = rs.getBigDecimal("nightly_rate");
        return new Room(number, type, capacity, nightlyRate);
    }
}
