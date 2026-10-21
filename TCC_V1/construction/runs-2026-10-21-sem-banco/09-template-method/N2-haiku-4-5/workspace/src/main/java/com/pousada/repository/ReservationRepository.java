package com.pousada.repository;

import com.pousada.domain.Reservation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Map<Long, Reservation> findAll() {
        return new HashMap<>(reservations);
    }

    public long getNextId() {
        return nextId.getAndIncrement();
    }
}
