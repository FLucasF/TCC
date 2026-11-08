package com.pousada.infrastructure;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public List<Room> findAll() throws Exception {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type FROM rooms ORDER BY number")) {

            while (rs.next()) {
                int number = rs.getInt("number");
                String type = rs.getString("type");
                rooms.add(new Room(number, RoomType.valueOf(type)));
            }
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT type FROM rooms WHERE number = " + number)) {

            if (rs.next()) {
                String type = rs.getString("type");
                return Optional.of(new Room(number, RoomType.valueOf(type)));
            }
        }
        return Optional.empty();
    }
}
