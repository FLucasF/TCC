package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

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

    public long nextId() {
        return idGenerator.getAndIncrement();
    }

    public boolean hasConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> excludeId == null || r.getId() != excludeId)
                .anyMatch(r -> overlaps(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isAfter(start2) ? false : !start1.isBefore(end2) ? false : true;
    }
}
