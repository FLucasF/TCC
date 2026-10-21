package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

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

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut))
                .toList();
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate date) {
        return reservations.stream()
                .filter(Reservation::isPaid)
                .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
                .toList();
    }

    public List<Reservation> findReservationsStartingInMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        return reservations.stream()
                .filter(r -> !r.getCheckIn().isBefore(start) && !r.getCheckIn().isAfter(end))
                .toList();
    }

    public long nextId() {
        return idGenerator.incrementAndGet();
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
