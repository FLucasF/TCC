package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.MessageRepository;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.DateUtils;
import com.pousada.utils.PriceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public static class CreateReservationRequest {
        public int roomNumber;
        public String guestName;
        public String guestEmail;
        public String guestCpf;
        public LocalDate checkIn;
        public LocalDate checkOut;
        public int guests;
        public String ratePlan;
    }

    public static Reservation createReservation(CreateReservationRequest req) {
        Room room = RoomRepository.findByNumber(req.roomNumber);
        if (room == null) {
            throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
        }

        if (req.guests < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least 1 guest required");
        }

        if (req.guests > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Guest count exceeds room capacity");
        }

        if (!req.checkOut.isAfter(req.checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (ReservationRepository.hasConflict(req.roomNumber, req.checkIn, req.checkOut)) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room not available for selected dates");
        }

        int nights = DateUtils.daysBetween(req.checkIn, req.checkOut);
        PriceCalculator.PriceResult price = PriceCalculator.calculate(
            room.getNightlyRate(), req.checkIn, req.checkOut, req.ratePlan
        );

        Reservation reservation = ReservationRepository.create(
            req.roomNumber,
            req.guestName,
            req.guestEmail,
            req.guestCpf,
            req.checkIn,
            req.checkOut,
            req.guests,
            req.ratePlan,
            nights,
            price.subtotal,
            price.discount,
            price.total
        );

        return reservation;
    }

    public static Reservation confirmReservation(int reservationId, BigDecimal amountPaid) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        ReservationRepository.updateStatus(reservationId, "CONFIRMED");
        ReservationRepository.updateAmountPaid(reservationId, amountPaid);

        Reservation updated = ReservationRepository.findById(reservationId);

        ReceiptService.sendConfirmationEmail(updated);

        return updated;
    }

    public static Reservation checkIn(int reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        ReservationRepository.updateStatus(reservationId, "CHECKED_IN");
        return ReservationRepository.findById(reservationId);
    }

    public static Reservation checkOut(int reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!"CHECKED_IN".equals(reservation.getStatus())) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        ReservationRepository.updateStatus(reservationId, "CHECKED_OUT");
        return ReservationRepository.findById(reservationId);
    }

    public static Reservation cancelReservation(int reservationId) {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        String status = reservation.getStatus();
        if (!("PENDING".equals(status) || "CONFIRMED".equals(status))) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending or confirmed reservations can be cancelled");
        }

        ReservationRepository.updateStatus(reservationId, "CANCELLED");

        BigDecimal refund = BigDecimal.ZERO;
        if ("CONFIRMED".equals(status)) {
            if ("STANDARD".equals(reservation.getRatePlan())) {
                refund = reservation.getAmountPaid();
            }
        }

        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            ReservationRepository.updateRefund(reservationId, refund);
        }

        return ReservationRepository.findById(reservationId);
    }
}
