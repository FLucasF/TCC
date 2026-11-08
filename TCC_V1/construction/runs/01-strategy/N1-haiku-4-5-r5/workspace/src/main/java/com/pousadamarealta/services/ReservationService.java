package com.pousadamarealta.services;

import com.pousadamarealta.database.MessageRepository;
import com.pousadamarealta.database.ReservationRepository;
import com.pousadamarealta.database.RoomRepository;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.Room;
import com.pousadamarealta.rateplans.CorporatePlan;
import com.pousadamarealta.rateplans.RatePlan;
import com.pousadamarealta.rateplans.RatePlanFactory;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    private static final String CONFIRMATION_EMAIL_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public static Reservation createReservation(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            String ratePlanName,
            String companyCode
    ) throws SQLException, RatePlan.InvalidRatePlanException {
        // Validate inputs
        if (guests < 1) {
            throw new IllegalArgumentException("Guest count must be at least 1");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        // Validate room exists
        Room room = RoomRepository.findById(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        // Validate guest count doesn't exceed capacity
        if (guests > room.capacity) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        // Validate rate plan
        RatePlan ratePlan = RatePlanFactory.getPlan(ratePlanName);
        if (ratePlan == null) {
            throw new IllegalArgumentException("Invalid rate plan");
        }

        // Validate rate plan specific rules
        ratePlan.validateAppliesTo(room, checkIn, checkOut, guests, companyCode);

        // Check room availability
        var conflictingReservations = ReservationRepository.findByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflictingReservations.isEmpty()) {
            throw new IllegalArgumentException("Room not available for these dates");
        }

        // Calculate price
        RatePlan.PriceCalculation priceCalc;
        if (ratePlan instanceof CorporatePlan) {
            priceCalc = ((CorporatePlan) ratePlan).calculatePrice(room, checkIn, checkOut, companyCode);
        } else {
            priceCalc = ratePlan.calculatePrice(room, checkIn, checkOut);
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        // Create reservation
        Reservation reservation = ReservationRepository.create(
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlanName,
            companyCode,
            nights,
            priceCalc.subtotal,
            priceCalc.discount,
            priceCalc.total
        );

        return reservation;
    }

    public static Reservation confirmReservation(int reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.status.equals("PENDING")) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }

        // Update status and amount paid
        ReservationRepository.updateConfirmation(reservationId, reservation.total);

        // Send confirmation email
        String comprovante = ReceiptService.generateReceipt(reservation);
        String emailBody = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.guestName,
            comprovante
        );

        MessageRepository.create("EMAIL", reservation.guestEmail, CONFIRMATION_EMAIL_SUBJECT, emailBody);

        return ReservationRepository.findById(reservationId);
    }

    public static Reservation cancelReservation(int reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        String status = reservation.status;
        if (!status.equals("PENDING") && !status.equals("CONFIRMED")) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        // Calculate refund
        RatePlan ratePlan = RatePlanFactory.getPlan(reservation.ratePlan);
        if (ratePlan == null) {
            throw new IllegalArgumentException("Invalid rate plan");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (status.equals("CONFIRMED")) {
            refund = ratePlan.getRefundAmount(reservation.amountPaid);
        }

        ReservationRepository.updateCancellation(reservationId, refund);

        return ReservationRepository.findById(reservationId);
    }

    public static Reservation checkIn(int reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.status.equals("CONFIRMED")) {
            throw new IllegalStateException("Only confirmed reservations can check in");
        }

        ReservationRepository.updateStatus(reservationId, "CHECKED_IN");

        return ReservationRepository.findById(reservationId);
    }

    public static Reservation checkOut(int reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        if (!reservation.status.equals("CHECKED_IN")) {
            throw new IllegalStateException("Only checked in reservations can check out");
        }

        ReservationRepository.updateStatus(reservationId, "CHECKED_OUT");

        return ReservationRepository.findById(reservationId);
    }
}
