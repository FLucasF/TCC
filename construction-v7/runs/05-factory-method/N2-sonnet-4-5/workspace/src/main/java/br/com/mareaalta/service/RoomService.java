package br.com.mareaalta.service;

import br.com.mareaalta.domain.Room;
import br.com.mareaalta.domain.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomService {
    private final List<Room> rooms;

    public RoomService() {
        this.rooms = new ArrayList<>();
        rooms.add(new Room("101", RoomType.STANDARD));
        rooms.add(new Room("102", RoomType.STANDARD));
        rooms.add(new Room("201", RoomType.DELUXE));
        rooms.add(new Room("202", RoomType.DELUXE));
        rooms.add(new Room("301", RoomType.SUITE));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public Optional<Room> getRoomByNumber(String number) {
        return rooms.stream()
            .filter(r -> r.getNumber().equals(number))
            .findFirst();
    }
}
