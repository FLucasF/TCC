package pousada.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pousada.domain.Room;
import pousada.domain.RoomType;

public class RoomRepository {
    private static final String COLUMNS = "SELECT number, type, capacity, nightly_rate FROM rooms";

    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    public List<Room> all() {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement(COLUMNS + " ORDER BY number");
                 var rows = statement.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rows.next()) {
                    rooms.add(map(rows));
                }
                return rooms;
            }
        });
    }

    public Optional<Room> find(int number) {
        return find(number, "");
    }

    /** Busca o quarto travando-o até o fim da transação, para serializar as reservas dele. */
    public Optional<Room> lock(int number) {
        return find(number, " FOR UPDATE");
    }

    private Optional<Room> find(int number, String suffix) {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement(COLUMNS + " WHERE number = ?" + suffix)) {
                statement.setInt(1, number);
                try (var rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.empty();
                }
            }
        });
    }

    private static Room map(ResultSet rows) throws SQLException {
        return new Room(rows.getInt("number"), RoomType.valueOf(rows.getString("type")),
                rows.getInt("capacity"), rows.getBigDecimal("nightly_rate"));
    }
}
