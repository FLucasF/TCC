package com.pousada.repositories;

import com.pousada.Database;
import com.pousada.models.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public static Room findByNumber(int number) {
        try (Connection conn = Database.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = " + number
            );

            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        } catch (SQLException e) {
            logger.error("Error finding room", e);
        }
        return null;
    }

    public static List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number"
            );

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    rs.getString("type"),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error listing rooms", e);
        }
        return rooms;
    }
}
