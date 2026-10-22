package com.pousadamareal.dao;

import com.pousadamareal.config.DatabaseConfig;
import com.pousadamareal.model.Room;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {
    public static List<Room> getAllRooms() throws Exception {
        List<Room> rooms = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getDouble("nightly_rate")
                ));
            }
        }

        return rooms;
    }

    public static Room getRoomByNumber(int number) throws Exception {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number)) {

            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getDouble("nightly_rate")
                );
            }
        }

        return null;
    }
}
