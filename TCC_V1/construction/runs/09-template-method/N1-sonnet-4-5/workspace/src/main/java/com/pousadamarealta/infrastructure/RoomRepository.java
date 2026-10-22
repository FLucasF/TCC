package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;

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
        String query = "SELECT number, type FROM rooms ORDER BY number";
        List<Room> rooms = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String number = rs.getString("number");
                RoomType type = RoomType.valueOf(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar quartos", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String query = "SELECT number, type FROM rooms WHERE number = ?";

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setString(1, number);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.valueOf(rs.getString("type"));
                    return Optional.of(new Room(number, type));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar quarto", e);
        }

        return Optional.empty();
    }
}
