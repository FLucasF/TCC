package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final List<Reservation> reservations;
    private final AtomicLong idGenerator;

    public ReservationRepository() {
        this.reservations = new ArrayList<>();
        this.idGenerator = new AtomicLong(0);
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

    public long nextId() {
        return idGenerator.incrementAndGet();
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> excludeReservationId == null || r.getId() != excludeReservationId)
                .noneMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2.minusDays(1)) && !start2.isAfter(end1.minusDays(1));
    }

    public List<Reservation> findOccupiedOnDate(LocalDate date) {
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        || r.getStatus() == ReservationStatus.CHECKED_IN
                        || r.getStatus() == ReservationStatus.CHECKED_OUT)
                .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
                .toList();
    }

    public List<Reservation> findStartingInMonth(int year, int month) {
        return reservations.stream()
                .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
                .filter(r -> {
                    java.math.BigDecimal revenue = r.getAmountPaid();
                    if (r.getRefund() != null) {
                        revenue = revenue.subtract(r.getRefund());
                    }
                    return revenue.compareTo(java.math.BigDecimal.ZERO) > 0;
                })
                .toList();
    }
}
