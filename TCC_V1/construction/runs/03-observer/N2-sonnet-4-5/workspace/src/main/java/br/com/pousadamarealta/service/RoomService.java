package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomByNumber(String number) {
        return roomRepository.findByNumber(number);
    }
}
