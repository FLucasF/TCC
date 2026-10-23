package br.com.pousada.maralta.service;

import br.com.pousada.maralta.domain.Room;
import br.com.pousada.maralta.repository.RoomRepository;

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
        return roomRepository.findByNumber(number);
    }
}
