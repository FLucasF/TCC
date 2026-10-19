package com.pousada.domain.service;

import com.pousada.domain.model.Room;
import com.pousada.domain.validation.ValidationException;
import com.pousada.infrastructure.repository.RoomRepository;

import java.util.List;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoomByNumber(String number) {
        return roomRepository.findByNumber(number)
            .orElseThrow(() -> new ValidationException("ROOM_NOT_FOUND",
                "Room " + number + " not found"));
    }
}
