package com.pousada.repository;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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

    public long nextId() {
        return idGenerator.getAndIncrement();
    }

    public List<Reservation> findActiveByRoomAndNight(String roomNumber, LocalDate night) {
        return reservations.stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.occupiesNight(night))
            .toList();
    }

    public List<Reservation> findPaidByNight(LocalDate night) {
        return reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> r.occupiesNight(night))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();
    }

    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        return reservations.stream()
            .filter(r -> !r.getCheckIn().isBefore(start) && !r.getCheckIn().isAfter(end))
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();
    }
}
