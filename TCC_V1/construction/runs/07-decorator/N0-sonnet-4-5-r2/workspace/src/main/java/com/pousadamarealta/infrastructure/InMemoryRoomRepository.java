package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.model.RoomType;
import com.pousadamarealta.domain.repository.RoomRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class InMemoryRoomRepository implements RoomRepository {

    private static final List<Room> ROOMS = Arrays.asList(
        new Room("101", RoomType.STANDARD),
        new Room("102", RoomType.STANDARD),
        new Room("201", RoomType.DELUXE),
        new Room("202", RoomType.DELUXE),
        new Room("301", RoomType.SUITE)
    );

    @Override
    public List<Room> findAll() {
        return ROOMS;
    }

    @Override
    public Optional<Room> findByNumber(String number) {
        return ROOMS.stream()
            .filter(room -> room.getNumber().equals(number))
            .findFirst();
    }
}
