package br.com.pousadamarealta.api;

import br.com.pousadamarealta.domain.room.Room;
import br.com.pousadamarealta.domain.room.RoomRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void list(Context ctx) throws SQLException {
        List<Room> rooms = roomRepository.findAll();

        List<Map<String, Object>> response = rooms.stream()
                .map(room -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("number", room.getNumber());
                    map.put("type", room.getType().name());
                    map.put("capacity", room.getCapacity());
                    map.put("nightlyRate", room.getNightlyRate());
                    return map;
                })
                .collect(Collectors.toList());

        ctx.json(response);
    }
}
