package com.pousadamaralta.domain.service;

import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.infrastructure.repository.RoomRepository;
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
            .orElseThrow(() -> new RoomNotFoundException(number));
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomNumber) {
            super("Room not found: " + roomNumber);
        }
    }
}
