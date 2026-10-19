package com.pousada.domain.validation;

import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.ReservationStatus;
import com.pousada.domain.model.Room;

import java.time.LocalDate;
import java.util.List;

public class ReservationValidator {

    public static void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("INVALID_REQUEST", "Check-out must be after check-in");
        }
    }

    public static void validateGuests(int guests, Room room) {
        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "At least one guest is required");
        }
        if (guests > room.getCapacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED",
                "Room capacity is " + room.getCapacity() + " but " + guests + " guests requested");
        }
    }

    public static void validateRoomAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut,
                                               List<Reservation> existingReservations) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            if (!existingReservations.isEmpty()) {
                throw new ValidationException("ROOM_UNAVAILABLE",
                    "Room " + roomNumber + " is not available for the requested dates");
            }
            current = current.plusDays(1);
        }
    }

    public static void validateConfirmation(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Only pending reservations can be confirmed");
        }
    }

    public static void validateCheckIn(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Only confirmed reservations can be checked in");
        }
    }

    public static void validateCheckOut(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Only checked-in reservations can be checked out");
        }
    }

    public static void validateCancellation(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Only pending or confirmed reservations can be cancelled");
        }
    }

    public static void validateDocumentGeneration(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Document can only be generated for paid reservations");
        }
    }
}
