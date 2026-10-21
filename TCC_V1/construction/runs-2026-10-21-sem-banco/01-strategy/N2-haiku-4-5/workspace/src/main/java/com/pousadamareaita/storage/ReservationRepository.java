package com.pousadamareaita.storage;

import com.pousadamareaita.model.Reservation;
import java.util.*;

public class ReservationRepository {
    private static final Map<Integer, Reservation> reservations = new LinkedHashMap<>();
    private static int nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation findById(int id) {
        return reservations.get(id);
    }

    public Collection<Reservation> findAll() {
        return reservations.values();
    }

    public int getNextId() {
        return nextId++;
    }
}
