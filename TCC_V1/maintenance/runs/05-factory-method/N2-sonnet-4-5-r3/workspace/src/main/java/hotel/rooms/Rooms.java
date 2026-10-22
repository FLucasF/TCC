package hotel.rooms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** The inn's rooms. They rarely change, so they come with the database schema. */
public final class Rooms {

    private final DataSource dataSource;

    public Rooms(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Room> all() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                ResultSet rows = select.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rows.next()) {
                rooms.add(read(rows));
            }
            return rooms;
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read the rooms", failure);
        }
    }

    public Optional<Room> find(int roomNumber) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            select.setInt(1, roomNumber);
            try (ResultSet rows = select.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read room " + roomNumber, failure);
        }
    }

    private static Room read(ResultSet row) throws SQLException {
        return new Room(
                row.getInt("number"),
                RoomType.valueOf(row.getString("type")),
                row.getInt("capacity"),
                row.getBigDecimal("nightly_rate"));
    }
}
