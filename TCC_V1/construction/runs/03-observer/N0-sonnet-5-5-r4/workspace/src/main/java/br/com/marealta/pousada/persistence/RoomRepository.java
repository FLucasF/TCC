package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomRepository {
    private static final String COLUMNS = "number, type, capacity, nightly_rate";
    private final Connection c;

    RoomRepository(Connection c) {
        this.c = c;
    }

    void insertIfAbsent(Room room) {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?,?,?,?) ON CONFLICT DO NOTHING")) {
            ps.setInt(1, room.number());
            ps.setString(2, room.type().name());
            ps.setInt(3, room.capacity());
            ps.setBigDecimal(4, room.nightlyRate());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Room> findAll() {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Busca o quarto travando a linha: quem reserva o mesmo quarto espera a transação acabar. */
    public Optional<Room> findForUpdate(int number) {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Room> find(int number) {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
