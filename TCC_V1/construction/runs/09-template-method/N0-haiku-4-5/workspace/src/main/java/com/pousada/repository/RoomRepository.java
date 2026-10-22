package com.pousada.repository;

import com.pousada.Database;
import com.pousada.model.Room;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public static Optional<Room> findByNumber(int number) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE number = " + number);
            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number");
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    private static Room mapRoom(ResultSet rs) throws Exception {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
