package pousada.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pousada.domain.Room;
import pousada.domain.RoomType;

public final class RoomRepository {
    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    public List<Room> all() {
        return db.transaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM rooms ORDER BY number");
                 var rs = ps.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rs.next()) {
                    rooms.add(map(rs));
                }
                return rooms;
            }
        });
    }

    public Optional<Room> find(int number) {
        return db.transaction(c -> {
            try (var ps = c.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
                ps.setInt(1, number);
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.<Room>empty();
                }
            }
        });
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
