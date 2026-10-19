package com.marealta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReservationService {
    private final Map<Integer, Room> rooms = new TreeMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<ReservationListener> listeners = new ArrayList<>();

    public ReservationService(List<Room> rooms) {
        rooms.forEach(r -> this.rooms.put(r.number(), r));
    }

    public void addListener(ReservationListener listener) {
        listeners.add(listener);
    }

    public List<Room> rooms() {
        return List.copyOf(rooms.values());
    }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan) {
        Room room = rooms.get(roomNumber);
        if (room == null) throw ApiException.roomNotFound(roomNumber);
        if (guests > room.capacity()) throw ApiException.capacityExceeded(room);
        boolean taken = reservations.stream().anyMatch(r -> r.room().equals(room)
                && r.status() != ReservationStatus.CANCELLED && r.overlaps(checkIn, checkOut));
        if (taken) throw ApiException.roomUnavailable(roomNumber);
        Reservation reservation = new Reservation(reservations.size() + 1, room, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, plan);
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(String id) {
        try {
            int index = Integer.parseInt(id);
            if (index >= 1 && index <= reservations.size()) return reservations.get(index - 1);
        } catch (NumberFormatException ignored) {
            // cai no não encontrado
        }
        throw ApiException.reservationNotFound(id);
    }

    public synchronized Reservation confirm(String id) {
        Reservation r = get(id);
        r.confirm();
        listeners.forEach(l -> l.onConfirmed(r));
        return r;
    }

    public synchronized Reservation cancel(String id) {
        Reservation r = get(id);
        boolean wasPaid = r.status().isPaid();
        r.cancel();
        if (wasPaid) listeners.forEach(l -> l.onPaidReservationCancelled(r));
        return r;
    }

    public synchronized Reservation checkIn(String id) {
        Reservation r = get(id);
        r.registerCheckIn();
        return r;
    }

    public synchronized Receipt checkOut(String id) {
        Reservation r = get(id);
        r.registerCheckOut();
        return Receipt.of(r);
    }

    public synchronized Receipt document(String id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) throw ApiException.notAllowed("Segunda via só existe para reserva paga");
        return Receipt.of(r);
    }

    public synchronized List<Reservation> snapshot() {
        return List.copyOf(reservations);
    }
}
