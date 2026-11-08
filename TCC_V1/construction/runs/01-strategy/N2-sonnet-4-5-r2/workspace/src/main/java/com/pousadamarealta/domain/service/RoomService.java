package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.infrastructure.database.RoomRepository;

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
        return roomRepository.findByNumber(number).orElse(null);
    }
}
