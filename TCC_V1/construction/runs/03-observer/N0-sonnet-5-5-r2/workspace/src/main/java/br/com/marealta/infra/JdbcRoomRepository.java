package br.com.marealta.infra;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.service.RoomRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRoomRepository implements RoomRepository {
    private static final String SELECT = "SELECT number, type, capacity, nightly_rate FROM rooms";

    private final Database db;

    public JdbcRoomRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Room> findAll() {
        try (PreparedStatement ps = db.connection().prepareStatement(SELECT + " ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Room> find(int number) {
        return findOne(SELECT + " WHERE number = ?", number);
    }

    @Override
    public Optional<Room> findForUpdate(int number) {
        return findOne(SELECT + " WHERE number = ? FOR UPDATE", number);
    }

    private Optional<Room> findOne(String sql, int number) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }
}
