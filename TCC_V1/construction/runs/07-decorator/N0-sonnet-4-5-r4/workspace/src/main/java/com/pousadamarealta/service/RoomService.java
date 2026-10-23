package com.pousadamarealta.service;

import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.RoomNotFoundException;

import java.util.List;

public class RoomService {
    private final RoomDAO roomDAO;

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
    }

    public List<Room> getAllRooms() {
        return roomDAO.findAll();
    }

    public Room getRoomByNumber(String number) {
        return roomDAO.findByNumber(number)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + number));
    }
}
