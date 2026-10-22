package br.com.marealta.persistence;

import br.com.marealta.domain.Room;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final List<Room> HOTEL_ROOMS = List.of(
            new Room(101, "STANDARD", 2, new BigDecimal("249.90")),
            new Room(102, "STANDARD", 2, new BigDecimal("249.90")),
            new Room(201, "DELUXE", 3, new BigDecimal("389.90")),
            new Room(202, "DELUXE", 3, new BigDecimal("389.90")),
            new Room(301, "SUITE", 4, new BigDecimal("579.90")));

    void seed(Connection connection) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (number) DO NOTHING";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Room room : HOTEL_ROOMS) {
                ps.setInt(1, room.number());
                ps.setString(2, room.type());
                ps.setInt(3, room.capacity());
                ps.setBigDecimal(4, room.nightlyRate());
                ps.executeUpdate();
            }
        }
    }

    public List<Room> findAll(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(read(rs));
            }
            return rooms;
        }
    }

    /** Trava a linha do quarto até o fim da transação, para serializar reservas do mesmo quarto. */
    public Optional<Room> findForUpdate(Connection connection, int number) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Room> find(Connection connection, int number) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private static Room read(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), rs.getString("type"), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
