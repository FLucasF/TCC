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

public class RoomRepository {
    private final Database db;

    public RoomRepository(Database db) {
        this.db = db;
    }

    public List<Room> findAll() {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT number, type FROM rooms ORDER BY number");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(map(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    static Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")));
    }
}
