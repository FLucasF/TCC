package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }

    public long getNextId() {
        return idGenerator.getAndIncrement();
    }

    public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        return reservations.values().stream()
            .filter(r -> excludeId == null || r.getId() != excludeId)
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(Reservation::isActive)
            .anyMatch(r -> periodsOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean periodsOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
