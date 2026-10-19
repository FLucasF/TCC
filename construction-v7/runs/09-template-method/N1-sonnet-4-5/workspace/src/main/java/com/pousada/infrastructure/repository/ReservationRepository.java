package com.pousada.infrastructure.repository;

import com.pousada.domain.model.Reservation;

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

    public Optional<Reservation> findById(Long id) {
        return reservations.stream()
            .filter(r -> r.getId().equals(id))
            .findFirst();
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations);
    }

    public List<Reservation> findActiveByRoomAndNight(String roomNumber, LocalDate night) {
        return reservations.stream()
            .filter(r -> r.isActive())
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.occupiesNight(night))
            .toList();
    }

    public Long nextId() {
        return idGenerator.getAndIncrement();
    }
}
