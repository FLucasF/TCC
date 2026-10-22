package com.pousadamarealta.service;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;

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
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + number));
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }
}
