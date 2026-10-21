package com.pousada.repository;

import com.pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

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

    public List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night) {
        return reservations.stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.occupiesNight(night))
            .toList();
    }

    public long nextId() {
        return idGenerator.getAndIncrement();
    }
}
