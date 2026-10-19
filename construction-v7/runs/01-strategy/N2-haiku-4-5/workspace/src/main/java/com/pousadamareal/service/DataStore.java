package com.pousadamareal.service;

import com.pousadamareal.domain.Message;
import com.pousadamareal.domain.Reservation;
import com.pousadamareal.domain.Room;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DataStore {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong reservationIdCounter = new AtomicLong(1);
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong messageIdCounter = new AtomicLong(1);
    private final Map<Integer, Room> rooms = new HashMap<>();

    public DataStore() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, Room.RoomType.STANDARD, 2, new java.math.BigDecimal("249.90")));
        rooms.put(102, new Room(102, Room.RoomType.STANDARD, 2, new java.math.BigDecimal("249.90")));
        rooms.put(201, new Room(201, Room.RoomType.DELUXE, 3, new java.math.BigDecimal("389.90")));
        rooms.put(202, new Room(202, Room.RoomType.DELUXE, 3, new java.math.BigDecimal("389.90")));
        rooms.put(301, new Room(301, Room.RoomType.SUITE, 4, new java.math.BigDecimal("579.90")));
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public long createReservation(Reservation reservation) {
        long id = reservationIdCounter.getAndIncrement();
        reservation.id = id;
        reservations.put(id, reservation);
        return id;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }

    public void updateReservation(long id, Reservation reservation) {
        reservations.put(id, reservation);
    }

    public long createMessage(String channel, String to, String subject, String body) {
        long id = messageIdCounter.getAndIncrement();
        Message message = new Message(id, channel, to, subject, body);
        messages.add(message);
        return id;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
