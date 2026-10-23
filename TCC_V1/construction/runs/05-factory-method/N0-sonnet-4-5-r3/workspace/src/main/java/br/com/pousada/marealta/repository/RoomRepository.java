package br.com.pousada.marealta.repository;

import br.com.pousada.marealta.model.Room;
import br.com.pousada.marealta.model.RoomType;
import br.com.pousada.marealta.util.DatabaseInitializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private final DatabaseInitializer db;

    public RoomRepository(DatabaseInitializer db) {
        this.db = db;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            ResultSet rs = stmt.executeQuery();
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
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                RoomType type = RoomType.valueOf(rs.getString("type"));
                return Optional.of(new Room(number, type));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar quarto", e);
        }
    }
}
