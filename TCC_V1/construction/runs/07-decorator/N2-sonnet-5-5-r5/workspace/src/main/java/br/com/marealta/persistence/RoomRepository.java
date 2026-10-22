package br.com.marealta.persistence;

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

    public List<Room> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(read(rs));
            }
            return rooms;
        }
    }

    /** Finds the room and locks it until the end of the transaction. */
    public Optional<Room> findForUpdate(Connection c, int number) throws SQLException {
        return find(c, number, " FOR UPDATE");
    }

    public Optional<Room> find(Connection c, int number) throws SQLException {
        return find(c, number, "");
    }

    private Optional<Room> find(Connection c, int number, String suffix) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM rooms WHERE number = ?" + suffix)) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private Room read(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
