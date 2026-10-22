package com.pousada.repositories;

import com.pousada.database.Database;
import com.pousada.models.Room;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }
        return rooms;
    }

    public Room findByNumber(int number) throws Exception {
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number)) {

            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        }
        return null;
    }
}
