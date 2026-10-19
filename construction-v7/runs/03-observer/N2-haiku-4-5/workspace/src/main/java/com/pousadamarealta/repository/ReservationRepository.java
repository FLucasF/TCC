package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findAll() {
        return reservations.values().stream().toList();
    }

    public long getNextId() {
        return nextId++;
    }
}
