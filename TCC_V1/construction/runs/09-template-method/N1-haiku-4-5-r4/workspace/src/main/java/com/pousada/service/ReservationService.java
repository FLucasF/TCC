package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.model.*;
import com.pousada.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final Database database;

    public ReservationService(Database database) {
        this.database = database;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) throws Exception {
        Room room = database.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new InvalidRequestException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("CAPACITY_EXCEEDED");
        }

        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new InvalidRequestException("INVALID_REQUEST");
        }

        List<Reservation> conflicts = database.getReservationsByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("ROOM_UNAVAILABLE");
        }

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan.name());
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);

        return database.createReservation(reservation);
    }

    public Reservation getReservation(long id) throws Exception {
        Reservation reservation = database.getReservationById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationAfterConfirmation(id, amountPaid);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        return reservation;
    }

    public Reservation checkInReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationStatus(id, ReservationStatus.CHECKED_IN);
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        return reservation;
    }

    public Reservation checkOutReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationStatus(id, ReservationStatus.CHECKED_OUT);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        return reservation;
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        database.updateReservationAfterCancellation(id, refund);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public static class RoomNotFoundException extends Exception {
        public final String code;
        public RoomNotFoundException(String code) {
            super();
            this.code = code;
        }
    }

    public static class ReservationNotFoundException extends Exception {
        public final String code;
        public ReservationNotFoundException(String code) {
            super();
            this.code = code;
        }
    }

    public static class InvalidRequestException extends Exception {
        public final String code;
        public InvalidRequestException(String code) {
            super();
            this.code = code;
        }
    }

    public static class CapacityExceededException extends Exception {
        public final String code;
        public CapacityExceededException(String code) {
            super();
            this.code = code;
        }
    }

    public static class RoomUnavailableException extends Exception {
        public final String code;
        public RoomUnavailableException(String code) {
            super();
            this.code = code;
        }
    }

    public static class OperationNotAllowedException extends Exception {
        public final String code;
        public OperationNotAllowedException(String code) {
            super();
            this.code = code;
        }
    }
}
