package br.com.pousada.repository;

import br.com.pousada.config.DatabaseConfig;
import br.com.pousada.model.Room;
import br.com.pousada.model.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public Optional<Room> findByNumber(int number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar quarto", e);
        }
        return Optional.empty();
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar quartos", e);
        }
        return rooms;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            RoomType.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getDouble("nightly_rate")
        );
    }
}
