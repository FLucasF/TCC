package com.pousada.marealto.service;

import com.pousada.marealto.domain.Room;
import com.pousada.marealto.repository.RoomRepository;

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
            .orElseThrow(() -> new RoomNotFoundException("Room " + number + " not found"));
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }
}
