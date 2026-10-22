package com.pousadamareaalta.service;

import com.pousadamareaalta.exception.RoomNotFoundException;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.model.RoomNumber;

import java.util.Arrays;
import java.util.List;

public class RoomService {

    public List<Room> getAllRooms() {
        return Arrays.stream(RoomNumber.values())
            .map(rn -> new Room(rn.getNumber(), rn.getType()))
            .toList();
    }

    public Room getRoom(int number) {
        try {
            RoomNumber roomNumber = RoomNumber.fromNumber(number);
            return new Room(roomNumber.getNumber(), roomNumber.getType());
        } catch (IllegalArgumentException e) {
            throw new RoomNotFoundException("Room not found: " + number);
        }
    }
}
