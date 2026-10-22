package br.com.pousadamareal.repository;

import br.com.pousadamareal.db.DatabaseConnection;
import br.com.pousadamareal.domain.Room;
import br.com.pousadamareal.domain.RoomType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {
    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT number, type FROM rooms ORDER BY number");
                while (rs.next()) {
                    int number = rs.getInt("number");
                    String type = rs.getString("type");
                    rooms.add(new Room(number, RoomType.valueOf(type)));
                }
            }
        }
        return rooms;
    }

    public Room findById(int number) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT number, type FROM rooms WHERE number = " + number);
                if (rs.next()) {
                    String type = rs.getString("type");
                    return new Room(number, RoomType.valueOf(type));
                }
            }
        }
        return null;
    }
}
