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
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }

    public Long nextId() {
        return idGenerator.getAndIncrement();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut))
                .collect(Collectors.toList());
    }

    public List<Reservation> findByNight(LocalDate night) {
        return reservations.values().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.getStatus() != ReservationStatus.PENDING)
                .filter(r -> !r.getCheckIn().isAfter(night) && r.getCheckOut().isAfter(night))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .collect(Collectors.toList());
    }

    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        return reservations.values().stream()
                .filter(r -> !r.getCheckIn().isBefore(start) && !r.getCheckIn().isAfter(end))
                .filter(r -> r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
                .collect(Collectors.toList());
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2.minusDays(1)) && !end1.minusDays(1).isBefore(start2);
    }
}
