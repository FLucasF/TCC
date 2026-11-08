package com.pousada.service;

import com.pousada.exception.ApiException;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.model.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                                String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                                int guests, String ratePlanStr) {
        validateCreateRequest(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlanStr);

        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
        }

        if (guests > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Too many guests for this room");
        }

        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        RatePlan ratePlan = RatePlan.fromString(ratePlanStr);
        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

        PricingService.PricingResult pricing = PricingService.calculatePrice(room, checkIn, checkOut, ratePlan);
        reservation.setNights(pricing.nights);
        reservation.setSubtotal(pricing.subtotal);
        reservation.setDiscount(pricing.discount);
        reservation.setTotal(pricing.total);

        return ReservationRepository.save(reservation);
    }

    public static Reservation confirm(Long reservationId, BigDecimal amountPaid) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only confirm pending reservations");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        ReservationRepository.update(reservation);
        EmailService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public static Reservation checkIn(Long reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation checkOut(Long reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only check out guests who are checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation cancel(Long reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                "Can only cancel pending or confirmed reservations");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refund = calculateRefund(reservation);
        reservation.setRefund(refund);

        ReservationRepository.update(reservation);

        return reservation;
    }

    private static BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }

        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            return reservation.getAmountPaid();
        }

        return BigDecimal.ZERO;
    }

    private static void validateCreateRequest(int roomNumber, String guestName, String guestEmail,
                                              String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                              int guests, String ratePlanStr) {
        if (guestName == null || guestName.trim().isEmpty() ||
            guestEmail == null || guestEmail.trim().isEmpty() ||
            guestCpf == null || guestCpf.trim().isEmpty() ||
            checkIn == null || checkOut == null ||
            ratePlanStr == null || ratePlanStr.trim().isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required fields");
        }

        if (guests < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (RatePlan.fromString(ratePlanStr) == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan");
        }
    }
}
