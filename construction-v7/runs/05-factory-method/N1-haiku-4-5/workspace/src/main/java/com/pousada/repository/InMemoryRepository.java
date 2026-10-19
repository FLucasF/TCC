package com.pousada.repository;

import com.pousada.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRepository {
    private static final InMemoryRepository INSTANCE = new InMemoryRepository();

    private final Map<Integer, Room> rooms = new HashMap<>();
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong reservationIdCounter = new AtomicLong(0);
    private final AtomicLong messageIdCounter = new AtomicLong(0);

    private InMemoryRepository() {
        initializeRooms();
    }

    public static InMemoryRepository getInstance() {
        return INSTANCE;
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public Room getRoomByNumber(int roomNumber) {
        return rooms.get(roomNumber);
    }

    public Collection<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public long saveReservation(Reservation reservation) {
        long id = reservationIdCounter.incrementAndGet();
        Reservation withId = new Reservation(id, reservation.getRoomNumber(),
            reservation.getGuestName(), reservation.getGuestEmail(),
            reservation.getGuestIdentification(), reservation.getCheckIn(),
            reservation.getCheckOut(), reservation.getGuests(),
            reservation.getRatePlan(), reservation.getNights(),
            reservation.getSubtotal(), reservation.getDiscount(),
            reservation.getTotal());
        reservations.put(id, withId);
        return id;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public long saveMessage(String channel, String to, String subject, String body) {
        long id = messageIdCounter.incrementAndGet();
        messages.add(new Message(id, Message.Channel.valueOf(channel), to, subject, body));
        return id;
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            LocalDate night = current;
            boolean hasConflict = reservations.values().stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .filter(r -> r.getStatus() != Reservation.Status.CANCELLED)
                .anyMatch(r -> !night.isBefore(r.getCheckIn()) && night.isBefore(r.getCheckOut()));

            if (hasConflict) {
                return false;
            }
            current = current.plusDays(1);
        }
        return true;
    }

    public List<Reservation> getReservationsByRoomAndDate(int roomNumber, LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != Reservation.Status.CANCELLED)
            .filter(r -> !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut()))
            .sorted(Comparator.comparingLong(Reservation::getId))
            .toList();
    }

    public List<Reservation> getReservationsByMonthAndStatus(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED ||
                         r.getStatus() == Reservation.Status.CHECKED_IN ||
                         r.getStatus() == Reservation.Status.CHECKED_OUT)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .toList();
    }

    public void resetForTesting() {
        reservations.clear();
        messages.clear();
        reservationIdCounter.set(0);
        messageIdCounter.set(0);
    }
}
