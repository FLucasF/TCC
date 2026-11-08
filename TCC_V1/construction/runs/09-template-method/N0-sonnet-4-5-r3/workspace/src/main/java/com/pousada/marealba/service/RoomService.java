package com.pousada.marealba.service;

import com.pousada.marealba.exception.ApiException;
import com.pousada.marealba.model.Room;
import com.pousada.marealba.repository.RoomRepository;

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
            .orElseThrow(ApiException::roomNotFound);
    }
}
