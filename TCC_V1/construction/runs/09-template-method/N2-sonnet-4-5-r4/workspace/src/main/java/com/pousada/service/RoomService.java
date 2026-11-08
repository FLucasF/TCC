package com.pousada.service;

import com.pousada.domain.Room;
import com.pousada.repository.RoomRepository;

import java.util.List;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoomByNumber(int number) {
        return roomRepository.findByNumber(number)
            .orElseThrow(() -> new RoomNotFoundException());
    }

    public static class RoomNotFoundException extends RuntimeException {
    }
}
