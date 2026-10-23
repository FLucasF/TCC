package br.com.maralta.repository;

import br.com.maralta.model.Room;
import br.com.maralta.model.RoomType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {

    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setNumber(rs.getString("number"));
                room.setType(RoomType.valueOf(rs.getString("type")));
                rooms.add(room);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> findByNumber(String number) {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, number);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setNumber(rs.getString("number"));
                    room.setType(RoomType.valueOf(rs.getString("type")));
                    return Optional.of(room);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }
}
