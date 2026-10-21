package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Reservation save(Reservation reservation) {
        reservations.add(reservation);
        return reservation;
    }

    public Optional<Reservation> findById(int id) {
        return reservations.stream()
            .filter(r -> r.id() == id)
            .findFirst();
    }

    public List<Reservation> findAll() {
        return List.copyOf(reservations);
    }

    public int nextId() {
        return nextId.getAndIncrement();
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, Integer excludeReservationId) {
        return reservations.stream()
            .filter(r -> r.roomNumber() == roomNumber)
            .filter(r -> r.status() != ReservationStatus.CANCELLED)
            .filter(r -> excludeReservationId == null || r.id() != excludeReservationId)
            .noneMatch(r -> datesOverlap(r.checkIn(), r.checkOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
