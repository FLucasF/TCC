package pousada.api;

import io.javalin.http.Context;
import pousada.domain.Room;
import pousada.repository.RoomRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> map = new HashMap<>();
                map.put("number", room.number());
                map.put("type", room.type().name());
                map.put("capacity", room.capacity());
                map.put("nightlyRate", room.type().nightlyRate());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
