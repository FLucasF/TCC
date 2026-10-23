package br.com.pousada.maralta.repository;

import br.com.pousada.maralta.database.Database;
import br.com.pousada.maralta.domain.Room;
import br.com.pousada.maralta.domain.RoomType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    private final Database database;

    public RoomRepository(Database database) {
        this.database = database;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms ORDER BY number")) {

            var rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Room findByNumber(String number) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT number, type FROM rooms WHERE number = ?")) {

            stmt.setString(1, number);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return new Room(
                    rs.getString("number"),
                    RoomType.valueOf(rs.getString("type"))
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return null;
    }
}
