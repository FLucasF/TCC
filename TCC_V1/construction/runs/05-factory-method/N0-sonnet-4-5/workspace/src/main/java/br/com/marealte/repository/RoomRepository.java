package br.com.marealte.repository;

import br.com.marealte.database.Database;
import br.com.marealte.model.Room;
import br.com.marealte.model.RoomType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setNumber(rs.getString("number"));
                room.setType(RoomType.valueOf(rs.getString("type")));
                room.setCapacity(rs.getInt("capacity"));
                room.setNightlyRate(rs.getBigDecimal("nightly_rate"));
                rooms.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Room findByNumber(String number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setNumber(rs.getString("number"));
                    room.setType(RoomType.valueOf(rs.getString("type")));
                    room.setCapacity(rs.getInt("capacity"));
                    room.setNightlyRate(rs.getBigDecimal("nightly_rate"));
                    return room;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return null;
    }
}
