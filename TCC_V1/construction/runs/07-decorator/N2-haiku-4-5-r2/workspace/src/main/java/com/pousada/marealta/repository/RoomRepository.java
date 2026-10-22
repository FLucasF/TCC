package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public void initialize() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM rooms";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                if (rs.getInt("count") == 0) {
                    insertDefaultRooms(conn);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar quartos", e);
        }
    }

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, number);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar quarto", e);
        }
        return Optional.empty();
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao listar quartos", e);
        }
        return rooms;
    }

    public String getRoomType(int roomNumber) {
        return findByNumber(roomNumber)
            .map(room -> room.getType().name())
            .orElse("UNKNOWN");
    }

    private void insertDefaultRooms(Connection conn) throws Exception {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            insertRoom(stmt, 101, "STANDARD", 2, "249.90");
            insertRoom(stmt, 102, "STANDARD", 2, "249.90");
            insertRoom(stmt, 201, "DELUXE", 3, "389.90");
            insertRoom(stmt, 202, "DELUXE", 3, "389.90");
            insertRoom(stmt, 301, "SUITE", 4, "579.90");
        }
    }

    private void insertRoom(PreparedStatement stmt, int number, String type, int capacity, String rate) throws Exception {
        stmt.setInt(1, number);
        stmt.setString(2, type);
        stmt.setInt(3, capacity);
        stmt.setBigDecimal(4, new BigDecimal(rate));
        stmt.addBatch();
        stmt.executeBatch();
    }

    private Room mapRow(ResultSet rs) throws Exception {
        return new Room(
            rs.getInt("number"),
            Room.Type.valueOf(rs.getString("type")),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
