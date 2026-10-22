package br.com.marealta.reservas.persistence;

import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.domain.RoomType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final List<Room> INITIAL_ROOMS = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));

    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    /** Cadastra os quartos da pousada se ainda não estiverem no banco. */
    public void seed() {
        db.inTransaction(() -> {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT (number) DO NOTHING")) {
                for (Room room : INITIAL_ROOMS) {
                    ps.setInt(1, room.number());
                    ps.setString(2, room.type().name());
                    ps.setInt(3, room.capacity());
                    ps.setBigDecimal(4, room.nightlyRate());
                    ps.executeUpdate();
                }
                return null;
            } catch (SQLException e) {
                throw new DatabaseException("Erro ao cadastrar quartos", e);
            }
        });
    }

    public List<Room> findAll() {
        return db.inTransaction(() -> {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rs.next()) {
                    rooms.add(map(rs));
                }
                return rooms;
            } catch (SQLException e) {
                throw new DatabaseException("Erro ao listar quartos", e);
            }
        });
    }

    /** Busca o quarto travando a linha, para serializar reservas concorrentes do mesmo quarto. */
    public Optional<Room> findForUpdate(int number) {
        Connection connection = db.connection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar quarto", e);
        }
    }

    public Optional<Room> find(int number) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar quarto", e);
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }
}
