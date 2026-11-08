package br.marealta.db;

import br.marealta.model.Room;
import br.marealta.model.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO {
    private final Database db;

    public RoomDAO(Database db) {
        this.db = db;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getBigDecimal("nightly_rate")
                );
                rooms.add(room);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number);
            if (rs.next()) {
                Room room = new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getBigDecimal("nightly_rate")
                );
                return Optional.of(room);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }
}
