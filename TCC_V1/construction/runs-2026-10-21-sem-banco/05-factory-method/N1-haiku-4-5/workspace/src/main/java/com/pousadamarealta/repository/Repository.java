package com.pousadamarealta.repository;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.util.*;

public class Repository {
    private final Map<Integer, Room> rooms = new HashMap<>();
    private final Map<Integer, Reservation> reservations = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private int nextReservationId = 1;
    private int nextMessageId = 1;

    public Repository() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public void saveReservation(Reservation reservation) {
        if (reservation.id == nextReservationId) {
            nextReservationId++;
        }
        reservations.put(reservation.id, reservation);
    }

    public Reservation getReservation(int id) {
        return reservations.get(id);
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public int getNextReservationId() {
        return nextReservationId;
    }

    public void saveMessage(Message message) {
        if (message.id == nextMessageId) {
            nextMessageId++;
        }
        messages.add(message);
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
