package com.pousada.repository;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations;
    private final AtomicLong nextId;

    public ReservationRepository() {
        this.reservations = new ArrayList<>();
        this.nextId = new AtomicLong(1);
    }

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

    public long getNextId() {
        return nextId.getAndIncrement();
    }

    public List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night) {
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
                .toList();
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        return reservations.stream()
                .filter(r -> {
                    LocalDate checkIn = r.getCheckIn();
                    return checkIn.getYear() == month.getYear() &&
                           checkIn.getMonthValue() == month.getMonthValue();
                })
                .toList();
    }
}
