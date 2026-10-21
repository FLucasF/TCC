package com.pousadamarealta.repository;

import com.pousadamarealta.model.Reservation;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Reservation save(Reservation reservation) {
        reservations.put(reservation.id, reservation);
        return reservation;
    }

    public Reservation create(Reservation reservation) {
        long id = nextId.getAndIncrement();
        Reservation newRes = new Reservation(
            id,
            reservation.roomNumber,
            reservation.guestName,
            reservation.guestEmail,
            reservation.guestCpf,
            reservation.checkIn,
            reservation.checkOut,
            reservation.guests,
            reservation.ratePlan,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total
        );
        reservations.put(id, newRes);
        return newRes;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Iterable<Reservation> findAll() {
        return reservations.values();
    }

    public Iterable<Reservation> findByRoomAndNight(int roomNumber, LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.roomNumber == roomNumber &&
                   !r.checkIn.isAfter(date) &&
                   !r.checkOut.isBefore(date.plusDays(1)) &&
                   !r.status.toString().equals("CANCELLED"))
            .toList();
    }

    public Iterable<Reservation> findByMonthStart(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.checkIn.getYear() == year && r.checkIn.getMonthValue() == month)
            .toList();
    }
}
