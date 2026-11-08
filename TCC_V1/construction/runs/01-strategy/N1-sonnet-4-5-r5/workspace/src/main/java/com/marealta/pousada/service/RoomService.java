package com.marealta.pousada.service;

import com.marealta.pousada.domain.model.Room;
import com.marealta.pousada.exception.RoomNotFoundException;
import com.marealta.pousada.repository.RoomRepository;

import java.sql.SQLException;
import java.util.List;

public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        try {
            return roomRepository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching rooms", e);
        }
    }

    public Room getRoomByNumber(int number) {
        try {
            return roomRepository.findByNumber(number)
                .orElseThrow(() -> new RoomNotFoundException("Room " + number + " not found"));
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching room", e);
        }
    }
}
