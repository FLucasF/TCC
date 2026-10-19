package com.pousadameraalta.repository;

import com.pousadameraalta.model.Room;
import com.pousadameraalta.model.Reservation;
import com.pousadameraalta.model.Message;

import java.math.BigDecimal;
import java.util.*;

public class DataStore {
    private static final DataStore instance = new DataStore();
    private final Map<Integer, Room> rooms = new HashMap<>();
    private Map<Long, Reservation> reservations = new HashMap<>();
    private List<Message> messages = new ArrayList<>();
    private long nextReservationId = 1;
    private long nextMessageId = 1;

    private DataStore() {
        initializeRooms();
    }

    public static DataStore getInstance() {
        return instance;
    }

    public synchronized void reset() {
        reservations = new HashMap<>();
        messages = new ArrayList<>();
        nextReservationId = 1;
        nextMessageId = 1;
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

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public synchronized Reservation saveReservation(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public synchronized Reservation createReservation(Reservation reservation) {
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }

    public synchronized Message saveMessage(String channel, String to, String subject, String body) {
        Message message = new Message(nextMessageId, channel, to, subject, body);
        messages.add(message);
        nextMessageId++;
        return message;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public synchronized long getNextReservationId() {
        long id = nextReservationId;
        nextReservationId++;
        return id;
    }
}
