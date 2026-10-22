package com.pousada.marealta.service;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {

    public static Reservation getReservation(long reservationId) throws SQLException {
        return ReservationRepository.findById(reservationId);
    }

    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                               LocalDate checkIn, LocalDate checkOut, int guests,
                                               String ratePlanStr, String guestCpf, String companyCnpj,
                                               String companyName, String guestPassport, String guestCountry)
            throws SQLException {

        // Validate room exists
        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        // Validate dates
        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Validate guests
        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        // Validate identification
        int identificationCount = 0;
        if (guestCpf != null) identificationCount++;
        if (companyCnpj != null) identificationCount++;
        if (guestPassport != null) identificationCount++;
        if (identificationCount != 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Check room availability
        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        // Parse rate plan
        Reservation.RatePlan ratePlan;
        try {
            ratePlan = Reservation.RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Create reservation
        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlan);
        reservation.setGuestCpf(guestCpf);
        reservation.setCompanyCnpj(companyCnpj);
        reservation.setCompanyName(companyName);
        reservation.setGuestPassport(guestPassport);
        reservation.setGuestCountry(guestCountry);

        // Calculate prices
        int nights = PricingService.calculateNights(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = PricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        boolean isNonRefundable = ratePlan == Reservation.RatePlan.NON_REFUNDABLE;
        BigDecimal discount = PricingService.calculateDiscount(subtotal, isNonRefundable);
        reservation.setDiscount(discount);

        BigDecimal total = PricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        // Save to database
        return ReservationRepository.create(reservation);
    }

    public static Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        ReservationRepository.update(reservation);

        // Send confirmation email
        MessageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public static Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING &&
            reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getStatus() == Reservation.Status.CONFIRMED &&
            reservation.getRatePlan() == Reservation.RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setRefund(refundAmount);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation checkIn(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CHECKED_IN);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation checkOut(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CHECKED_OUT);
        ReservationRepository.update(reservation);

        return reservation;
    }
}
