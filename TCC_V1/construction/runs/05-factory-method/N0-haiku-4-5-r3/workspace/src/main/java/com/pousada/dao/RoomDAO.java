package com.pousada.dao;

import com.pousada.config.DatabaseConfig;
import com.pousada.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO {
    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);

    public Optional<Room> findByNumber(int number) {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM rooms WHERE number = " + number
            );

            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error fetching room {}", number, e);
            throw new RuntimeException(e);
        }
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number");

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all rooms", e);
            throw new RuntimeException(e);
        }
        return rooms;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("number"),
            rs.getString("type"),
            rs.getInt("capacity"),
            rs.getBigDecimal("nightly_rate")
        );
    }
}
