package com.pousada.repository;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;

import java.time.LocalDate;
import java.util.*;

public class ReservationRepository {
    private final Map<Long, Reservation> reservations = new LinkedHashMap<>();
    private long nextId = 1;

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == 0) {
            // Create new reservation
            long id = nextId++;
            Reservation newReservation = new Reservation(
                id,
                reservation.getRoomNumber(),
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                reservation.getGuestCpf(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getGuests(),
                reservation.getRatePlan(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
            );
            newReservation.setStatus(reservation.getStatus());
            newReservation.setAmountPaid(reservation.getAmountPaid());
            newReservation.setRefund(reservation.getRefund());
            reservations.put(id, newReservation);
            return newReservation;
        } else {
            // Update existing reservation
            reservations.put(reservation.getId(), reservation);
            return reservation;
        }
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Collection<Reservation> findAll() {
        return reservations.values();
    }

    public Collection<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getRoomNumber() == roomNumber && r.getStatus() != ReservationStatus.CANCELLED) {
                if (!r.getCheckOut().isBefore(checkIn) && !r.getCheckIn().isAfter(checkOut.minusDays(1))) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public Collection<Reservation> findByMonth(int year, int month) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month) {
                result.add(r);
            }
        }
        return result;
    }
}
