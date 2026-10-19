package com.pousada.repository;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations;
    private final AtomicLong idGenerator;

    public ReservationRepository() {
        this.reservations = new ArrayList<>();
        this.idGenerator = new AtomicLong(1);
    }

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

    public long generateId() {
        return idGenerator.getAndIncrement();
    }

    public boolean hasConflict(int roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        return reservations.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> excludeId == null || r.getId() != excludeId)
                .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
