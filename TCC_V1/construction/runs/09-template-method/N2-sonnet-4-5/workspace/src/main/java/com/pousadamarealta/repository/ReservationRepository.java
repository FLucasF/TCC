package com.pousadamarealta.repository;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new LinkedHashMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }

    public List<Reservation> findActiveByRoomAndNight(String roomNumber, LocalDate night) {
        return reservations.values().stream()
                .filter(r -> r.isActive() && r.getRoomNumber().equals(roomNumber) && r.occupiesNight(night))
                .collect(Collectors.toList());
    }

    public List<Reservation> findPaidByNight(LocalDate night) {
        return reservations.values().stream()
                .filter(r -> r.isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .collect(Collectors.toList());
    }

    public List<Reservation> findByCheckInMonth(YearMonth month) {
        return reservations.values().stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .collect(Collectors.toList());
    }

    public List<Reservation> findPaidByCheckInMonth(YearMonth month) {
        return findByCheckInMonth(month).stream()
                .filter(Reservation::isPaid)
                .collect(Collectors.toList());
    }

    public long nextId() {
        return nextId++;
    }
}
