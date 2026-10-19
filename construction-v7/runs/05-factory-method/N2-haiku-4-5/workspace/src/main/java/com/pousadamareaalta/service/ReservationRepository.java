package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Reservation;

import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation findById(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> findAll() {
        return reservations.values();
    }

    public long generateId() {
        return nextId++;
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getRoomNumber() == roomNumber && datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut)) {
                result.add(r);
            }
        }
        return result;
    }

    private boolean datesOverlap(LocalDate r1Start, LocalDate r1End, LocalDate r2Start, LocalDate r2End) {
        return r1Start.isBefore(r2End) && r2Start.isBefore(r1End);
    }

    public List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getRoomNumber() == roomNumber) {
                if (!date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut())) {
                    result.add(r);
                }
            }
        }
        return result;
    }
}
