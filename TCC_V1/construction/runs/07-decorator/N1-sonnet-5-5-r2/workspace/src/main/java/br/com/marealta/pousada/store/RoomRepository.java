package br.com.marealta.pousada.store;

import br.com.marealta.pousada.domain.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public List<Room> findAll(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM rooms ORDER BY number");
             ResultSet rs = s.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(map(rs));
            }
            return rooms;
        }
    }

    /** Busca o quarto travando a linha, para serializar reservas concorrentes do mesmo quarto. */
    public Optional<Room> findForUpdate(Connection c, int number) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM rooms WHERE number = ? FOR UPDATE")) {
            s.setInt(1, number);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Room> find(Connection c, int number) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {
            s.setInt(1, number);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), rs.getString("type"), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
