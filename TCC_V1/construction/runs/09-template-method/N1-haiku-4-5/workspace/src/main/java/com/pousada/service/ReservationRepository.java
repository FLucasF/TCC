package com.pousada.service;

import com.pousada.domain.Reservation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }

    public long getNextId() {
        return nextId++;
    }
}
