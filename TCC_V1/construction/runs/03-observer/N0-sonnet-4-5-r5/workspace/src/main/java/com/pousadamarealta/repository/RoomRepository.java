package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM rooms ORDER BY number")) {

            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar quartos", e);
        }
        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar quarto", e);
        }
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        String number = rs.getString("number");
        RoomType type = RoomType.valueOf(rs.getString("type"));
        return new Room(number, type);
    }
}
