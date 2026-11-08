package com.marealata.service;

import com.marealata.dao.*;
import com.marealata.model.*;
import com.marealata.util.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                                String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                                int guests, RatePlan ratePlan) throws SQLException {
        validateReservation(roomNumber, checkIn, checkOut, guests);

        Room room = RoomDao.findByNumber(roomNumber);
        if (room == null) {
            throw new ReservationException("ROOM_NOT_FOUND", "Room " + roomNumber + " not found");
        }

        if (guests > room.capacity) {
            throw new ReservationException("CAPACITY_EXCEEDED", "Guests exceed room capacity");
        }

        List<Reservation> conflicting = ReservationDao.findByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflicting.isEmpty()) {
            throw new ReservationException("ROOM_UNAVAILABLE", "Room not available for these dates");
        }

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(roomNumber, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf,
                                                  checkIn, checkOut, guests, ratePlan,
                                                  nights, subtotal, discount, total);

        return ReservationDao.save(reservation);
    }

    public static Reservation confirmReservation(long id, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = ReservationDao.findById(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != ReservationStatus.PENDING) {
            throw new ReservationException("OPERATION_NOT_ALLOWED",
                "Cannot confirm a " + reservation.status + " reservation");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = amountPaid;
        ReservationDao.save(reservation);

        Room room = RoomDao.findByNumber(reservation.roomNumber);
        String roomType = room != null ? room.type : "UNKNOWN";

        MessageService.sendConfirmationEmail(reservation, roomType);

        return reservation;
    }

    public static Reservation checkIn(long id) throws SQLException {
        Reservation reservation = ReservationDao.findById(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED",
                "Cannot check in a " + reservation.status + " reservation");
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        return ReservationDao.save(reservation);
    }

    public static Reservation checkOut(long id) throws SQLException {
        Reservation reservation = ReservationDao.findById(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new ReservationException("OPERATION_NOT_ALLOWED",
                "Cannot check out a " + reservation.status + " reservation");
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        return ReservationDao.save(reservation);
    }

    public static Reservation cancelReservation(long id) throws SQLException {
        Reservation reservation = ReservationDao.findById(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != ReservationStatus.PENDING &&
            reservation.status != ReservationStatus.CONFIRMED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED",
                "Cannot cancel a " + reservation.status + " reservation");
        }

        ReservationStatus previousStatus = reservation.status;
        reservation.status = ReservationStatus.CANCELLED;

        if (previousStatus == ReservationStatus.CONFIRMED &&
            reservation.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                reservation.refund = reservation.amountPaid;
            } else {
                reservation.refund = BigDecimal.ZERO;
            }
        } else {
            reservation.refund = BigDecimal.ZERO;
        }

        return ReservationDao.save(reservation);
    }

    private static void validateReservation(int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                            int guests) {
        if (guests < 1) {
            throw new ReservationException("INVALID_REQUEST", "At least 1 guest is required");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ReservationException("INVALID_REQUEST", "Check-out must be after check-in");
        }
    }
}
