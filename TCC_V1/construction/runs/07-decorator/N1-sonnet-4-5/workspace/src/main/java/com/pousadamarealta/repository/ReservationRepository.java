package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.occupiesNight(night))
                .collect(Collectors.toList());
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        return reservations.values().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.isPaid())
                .filter(r -> r.occupiesNight(night))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .collect(Collectors.toList());
    }

    public List<Reservation> findReservationsStartingInMonth(YearMonth month) {
        return reservations.values().stream()
                .filter(r -> {
                    YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                    return checkInMonth.equals(month);
                })
                .filter(r -> r.getAmountPaid().compareTo(r.getRefund() != null ? r.getRefund() : r.getAmountPaid().multiply(BigDecimal.ZERO)) > 0)
                .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
                .collect(Collectors.toList());
    }

    public long nextId() {
        return nextId.getAndIncrement();
    }
}
