package br.com.pousadamarealta.repository;

import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.domain.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = database.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms", e);
        }
    }

    public Room findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRoom(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room", e);
        }
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
