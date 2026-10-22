package com.pousada.marealta.infrastructure.persistence;

import com.pousada.marealta.domain.Room;
import com.pousada.marealta.domain.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int number = rs.getInt("number");
                RoomType type = RoomType.fromString(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        }
        return rooms;
    }

    public Room findByNumber(int number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.fromString(rs.getString("type"));
                    return new Room(number, type);
                }
            }
        }
        return null;
    }
}
