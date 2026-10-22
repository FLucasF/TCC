package br.com.marealta.persistence;

import br.com.marealta.domain.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {
    private static final String COLUMNS = "number, type, capacity, nightly_rate";

    public List<Room> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM rooms ORDER BY number");
             ResultSet rows = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rows.next()) {
                rooms.add(read(rows));
            }
            return rooms;
        }
    }

    /** Busca o quarto travando a linha, para que reservas simultâneas do mesmo quarto sejam atendidas em fila. */
    public Optional<Room> findForUpdate(Connection connection, int number) throws SQLException {
        return find(connection, number, " FOR UPDATE");
    }

    public Optional<Room> find(Connection connection, int number) throws SQLException {
        return find(connection, number, "");
    }

    private Optional<Room> find(Connection connection, int number, String suffix) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + COLUMNS + " FROM rooms WHERE number = ?" + suffix)) {
            statement.setInt(1, number);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        }
    }

    private Room read(ResultSet rows) throws SQLException {
        return new Room(rows.getInt("number"), rows.getString("type"), rows.getInt("capacity"),
                rows.getBigDecimal("nightly_rate"));
    }
}
