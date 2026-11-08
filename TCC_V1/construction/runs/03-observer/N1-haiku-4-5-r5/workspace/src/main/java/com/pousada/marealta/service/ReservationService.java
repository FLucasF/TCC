package com.pousada.marealta.service;

import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.enums.RatePlan;
import com.pousada.marealta.enums.ReservationStatus;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.notification.NotificationManager;
import com.pousada.marealta.notification.ReservationEvent;
import com.pousada.marealta.util.PricingCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                               String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                               int guests, RatePlan ratePlan) throws Exception {
        validateReservationInput(roomNumber, checkIn, checkOut, guests, ratePlan);

        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        var conflictingReservations = ReservationRepository.findByRoomAndDates(
            roomNumber, checkIn, checkOut,
            ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT
        );

        if (!conflictingReservations.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = PricingCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        return ReservationRepository.create(
            roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total
        );
    }

    public static Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatusAndAmountPaid(reservationId, ReservationStatus.CONFIRMED, amountPaid);
        Reservation confirmed = ReservationRepository.findById(reservationId);

        NotificationManager.notify(new ReservationEvent(ReservationEvent.Type.CONFIRMED, confirmed));

        return confirmed;
    }

    public static Reservation checkIn(long reservationId) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatus(reservationId, ReservationStatus.CHECKED_IN);
        return ReservationRepository.findById(reservationId);
    }

    public static Reservation checkOut(long reservationId) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatus(reservationId, ReservationStatus.CHECKED_OUT);
        return ReservationRepository.findById(reservationId);
    }

    public static Reservation cancelReservation(long reservationId) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;

        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        ReservationRepository.updateRefund(reservationId, refund);
        ReservationRepository.updateStatus(reservationId, ReservationStatus.CANCELLED);

        Reservation cancelled = ReservationRepository.findById(reservationId);

        if (status == ReservationStatus.CONFIRMED) {
            NotificationManager.notify(new ReservationEvent(ReservationEvent.Type.CANCELLED, cancelled));
        }

        return cancelled;
    }

    private static void validateReservationInput(int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                                int guests, RatePlan ratePlan) throws Exception {
        if (roomNumber <= 0 || checkIn == null || checkOut == null || ratePlan == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }
}
