package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        try {
            return roomRepository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve rooms", e);
        }
    }

    public Optional<Room> getRoomByNumber(String number) {
        try {
            return roomRepository.findByNumber(number);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve room", e);
        }
    }
}
