package br.com.marealta.db;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {

    private static final String COLUMNS = "number, type, capacity, nightly_rate";

    public List<Room> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) rooms.add(map(rs));
            return rooms;
        }
    }

    public Optional<Room> find(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Takes a row lock on the room, serializing concurrent bookings of the same room. */
    public void lock(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            ps.executeQuery().close();
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
