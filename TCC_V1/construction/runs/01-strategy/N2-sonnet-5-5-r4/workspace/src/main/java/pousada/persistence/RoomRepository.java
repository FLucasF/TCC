package pousada.persistence;

import pousada.domain.Room;
import pousada.domain.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final String COLUMNS = "number, type, capacity, nightly_rate";

    public List<Room> findAll(Connection c) {
        return Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rs.next()) rooms.add(map(rs));
                return rooms;
            }
        });
    }

    public Optional<Room> find(Connection c, int number) {
        return query(c, "SELECT " + COLUMNS + " FROM rooms WHERE number = ?", number);
    }

    /** Trava a linha do quarto até o fim da transação, para serializar reservas do mesmo quarto. */
    public Optional<Room> findForUpdate(Connection c, int number) {
        return query(c, "SELECT " + COLUMNS + " FROM rooms WHERE number = ? FOR UPDATE", number);
    }

    private Optional<Room> query(Connection c, String sql, int number) {
        return Sql.unchecked(() -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, number);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
