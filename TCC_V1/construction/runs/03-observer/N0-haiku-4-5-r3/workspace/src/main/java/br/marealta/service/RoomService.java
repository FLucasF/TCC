package br.marealta.service;

import br.marealta.db.RoomDAO;
import br.marealta.model.Room;

import java.util.List;

public class RoomService {
    private final RoomDAO roomDAO;

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
    }

    public List<Room> getAllRooms() {
        return roomDAO.findAll();
    }
}
