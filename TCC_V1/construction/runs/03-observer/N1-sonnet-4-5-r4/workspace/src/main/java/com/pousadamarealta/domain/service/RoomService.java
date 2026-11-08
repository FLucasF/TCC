package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.sql.SQLException;
import java.util.List;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() throws SQLException {
        return roomRepository.findAll();
    }

    public Room getRoomByNumber(String number) throws SQLException {
        return roomRepository.findByNumber(number)
            .orElseThrow(() -> new RoomNotFoundException("Room " + number + " not found"));
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }
}
