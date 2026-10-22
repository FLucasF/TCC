package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {

    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                                String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                                int guests, RatePlan ratePlan) {
        // Validations
        if (checkOut.compareTo(checkIn) <= 0) {
            throw new IllegalArgumentException("Checkout must be after checkin");
        }

        var roomOpt = RoomRepository.findByNumber(roomNumber);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        var room = roomOpt.get();
        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        // Calculate pricing
        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        BigDecimal nightlyPrice = PricingCalculator.calculateNightlyPrice(room.getNightlyRate(), checkIn);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(nightlyPrice, nights);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        // Create reservation
        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total
        );

        return ReservationRepository.create(reservation);
    }

    public static Reservation confirmReservation(int reservationId, BigDecimal amountPaid) {
        var reservationOpt = ReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        ReservationRepository.update(reservation);

        // Send confirmation email
        EmailService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public static Reservation checkIn(int reservationId) {
        var reservationOpt = ReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation checkOut(int reservationId) {
        var reservationOpt = ReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation cancelReservation(int reservationId) {
        var reservationOpt = ReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        // Calculate refund before changing status
        ReservationStatus oldStatus = reservation.getStatus();
        if (oldStatus == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        ReservationRepository.update(reservation);

        return reservation;
    }
}
