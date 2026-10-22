package br.com.pousadamareal.api.handlers;

import br.com.pousadamareal.domain.Room;
import br.com.pousadamareal.repository.RoomRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;

public class RoomHandler {
    private final RoomRepository roomRepository;

    public RoomHandler(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void getAllRooms(Context ctx) throws SQLException {
        List<Room> rooms = roomRepository.findAll();
        JsonArray jsonArray = new JsonArray();

        for (Room room : rooms) {
            JsonObject json = new JsonObject();
            json.addProperty("number", room.getNumber());
            json.addProperty("type", room.getType().displayName());
            json.addProperty("capacity", room.getCapacity());
            json.addProperty("nightlyRate", Math.round(room.getNightlyRate() * 100) / 100.0);
            jsonArray.add(json);
        }

        ctx.json(jsonArray);
    }
}
