package br.com.marealta.reservation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    /** Os quartos da pousada; só entram no banco se ele ainda não os tiver. */
    private static final List<Room> INITIAL_ROOMS = List.of(
            new Room(101, "STANDARD", 2, new BigDecimal("249.90")),
            new Room(102, "STANDARD", 2, new BigDecimal("249.90")),
            new Room(201, "DELUXE", 3, new BigDecimal("389.90")),
            new Room(202, "DELUXE", 3, new BigDecimal("389.90")),
            new Room(301, "SUITE", 4, new BigDecimal("579.90")));

    public void seed(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into rooms (number, type, capacity, nightly_rate) values (?, ?, ?, ?) on conflict do nothing")) {
            for (Room room : INITIAL_ROOMS) {
                statement.setInt(1, room.number());
                statement.setString(2, room.type());
                statement.setInt(3, room.capacity());
                statement.setBigDecimal(4, room.nightlyRate());
                statement.executeUpdate();
            }
        }
    }

    public List<Room> all(Connection connection) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select number, type, capacity, nightly_rate from rooms order by number");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rooms.add(map(rs));
            }
        }
        return rooms;
    }

    /** Trava a linha do quarto: reservas simultâneas do mesmo quarto passam uma de cada vez. */
    public Optional<Room> findForUpdate(Connection connection, int number) throws SQLException {
        return find(connection, "select number, type, capacity, nightly_rate from rooms where number = ? for update", number);
    }

    public Optional<Room> find(Connection connection, int number) throws SQLException {
        return find(connection, "select number, type, capacity, nightly_rate from rooms where number = ?", number);
    }

    private Optional<Room> find(Connection connection, String sql, int number) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, number);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), rs.getString("type"), rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
