package br.com.pousadamarealta.reservas.persistence;

import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.domain.RoomType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    public List<Room> findAll() {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(map(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /** Com forUpdate, trava o quarto até o fim da transação: serializa reservas concorrentes dele. */
    public Optional<Room> find(int number, boolean forUpdate) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?"
                        + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
