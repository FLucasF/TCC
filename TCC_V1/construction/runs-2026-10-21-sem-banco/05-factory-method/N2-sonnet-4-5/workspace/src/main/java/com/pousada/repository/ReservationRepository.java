package com.pousada.repository;

import com.pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.add(reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return reservations.stream()
            .filter(r -> r.getId() == id)
            .findFirst();
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations);
    }

    public long nextId() {
        return nextId.getAndIncrement();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != com.pousada.domain.ReservationStatus.CANCELLED)
            .filter(r -> overlaps(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut))
            .toList();
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
