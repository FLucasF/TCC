package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new TreeMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation findById(long id) {
        return reservations.get(id);
    }

    public Iterable<Reservation> findAll() {
        return reservations.values();
    }

    public long getNextId() {
        long id = nextId;
        nextId++;
        return id;
    }

    public Iterable<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber && r.occupiesNight(date))
            .toList();
    }

    public Iterable<Reservation> findByMonthStarting(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .sorted((a, b) -> {
                int dateComparison = a.getCheckIn().compareTo(b.getCheckIn());
                if (dateComparison == 0) {
                    return Long.compare(a.getId(), b.getId());
                }
                return dateComparison;
            })
            .toList();
    }
}
