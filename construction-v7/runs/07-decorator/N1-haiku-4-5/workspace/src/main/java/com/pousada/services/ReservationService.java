package com.pousada.services;

import com.pousada.models.RatePlan;
import com.pousada.models.Reservation;
import com.pousada.models.ReservationStatus;
import com.pousada.models.Room;
import com.pousada.utils.DateUtils;
import com.pousada.utils.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final RoomService roomService;
    private long nextReservationId = 1;

    public ReservationService(RoomService roomService) {
        this.roomService = roomService;
    }

    public Reservation createReservation(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) throws IllegalArgumentException {

        Room room = roomService.getRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        long nights = DateUtils.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, room.getNightlyRate());
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
                nextReservationId++,
                roomNumber,
                guestName,
                guestEmail,
                guestCpf,
                checkIn,
                checkOut,
                guests,
                ratePlan,
                nights,
                subtotal,
                discount,
                total);

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public void confirmReservation(long id, BigDecimal amountPaid) throws IllegalArgumentException {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        try {
            reservation.confirm(amountPaid);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
    }

    public void checkInReservation(long id) throws IllegalArgumentException {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
    }

    public void checkOutReservation(long id) throws IllegalArgumentException {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
    }

    public void cancelReservation(long id) throws IllegalArgumentException {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        try {
            BigDecimal refund;
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            } else {
                refund = BigDecimal.ZERO;
            }
            reservation.cancel(refund);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservations.values()) {
            if (res.getRoomNumber() != roomNumber) {
                continue;
            }

            if (res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            if (DateUtils.dateIsInRange(checkIn, res.getCheckIn(), res.getCheckOut())) {
                return true;
            }

            if (DateUtils.dateIsInRange(checkOut, res.getCheckIn(), res.getCheckOut())) {
                return true;
            }

            if (!checkIn.isAfter(res.getCheckIn()) && !checkOut.isBefore(res.getCheckOut())) {
                return true;
            }
        }

        return false;
    }

    public List<Reservation> getReservationsForRoom(int roomNumber, LocalDate date) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> DateUtils.dateIsInRange(date, r.getCheckIn(), r.getCheckOut()))
                .toList();
    }

    public List<Reservation> getReservationsForMonth(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        return reservations.values().stream()
                .filter(r -> !r.getCheckIn().isBefore(monthStart) && r.getCheckIn().isBefore(monthEnd))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED || r.getAmountPaid().signum() > 0)
                .toList();
    }
}
