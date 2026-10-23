package com.pousadamarealta.service;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() throws SQLException {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomByNumber(String number) throws SQLException {
        return roomRepository.findByNumber(number);
    }
}
