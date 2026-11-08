package com.pousada.services;

import com.pousada.database.Database;
import com.pousada.models.Room;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private final Database db;

    public RoomService(Database db) {
        this.db = db;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                rooms.add(new Room(
                        rs.getInt("number"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar quartos", e);
        }
        return rooms;
    }

    public Room getRoomByNumber(int number) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number;
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return new Room(
                        rs.getInt("number"),
                        rs.getString("type"),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar quarto", e);
        }
        return null;
    }
}
