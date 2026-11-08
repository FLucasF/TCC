package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.domain.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    public List<Room> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(map(rs));
            }
            return rooms;
        }
    }

    /** Busca o quarto travando a linha: serializa reservas concorrentes do mesmo quarto. */
    public Optional<Room> findForUpdate(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Room> find(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
