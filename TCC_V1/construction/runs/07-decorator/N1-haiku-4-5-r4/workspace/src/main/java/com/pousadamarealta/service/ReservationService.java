package com.pousadamarealta.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.pousadamarealta.dao.ReservationDAO;
import com.pousadamarealta.dao.RoomDAO;
import com.pousadamarealta.models.*;
import com.pousadamarealta.util.PricingCalculator;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan)
            throws SQLException {
        Room room = RoomDAO.getRoomByNumber(roomNumber);
        if (room == null) {
            return null;
        }

        if (!ReservationDAO.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            return null;
        }

        if (guests < 1 || guests > room.capacity()) {
            return null;
        }

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        if (nights <= 0) {
            return null;
        }

        BigDecimal nightlyRate = room.nightlyRate();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal rate = PricingCalculator.calculateNightlyRate(nightlyRate, nightDate);
            subtotal = subtotal.add(rate);
        }
        subtotal = com.pousadamarealta.util.PriceRounding.round(subtotal);

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        return ReservationDAO.create(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                                     guests, ratePlan, nights, subtotal, discount, total);
    }

    public static Reservation getReservation(int id) throws SQLException {
        return ReservationDAO.getById(id);
    }

    public static Reservation confirmReservation(int id) throws SQLException {
        Reservation reservation = ReservationDAO.getById(id);
        if (reservation == null || reservation.status() != ReservationStatus.PENDING) {
            return null;
        }

        ReservationDAO.updateStatusAndAmountPaid(id, ReservationStatus.CONFIRMED, reservation.total());

        String emailBody = ReceiptFormatter.formatConfirmationEmail(
            reservation.guestName(),
            ReceiptFormatter.toReceipt(id, reservation.guestName(), reservation.guestCpf(),
                                      reservation.roomNumber(),
                                      RoomDAO.getRoomByNumber(reservation.roomNumber()).type(),
                                      reservation.checkIn(), reservation.checkOut(),
                                      reservation.nights(), reservation.subtotal(),
                                      reservation.discount(), reservation.total())
        );

        MessageService.sendEmail(reservation.guestEmail(),
                                "Reserva confirmada - Pousada Maré Alta",
                                emailBody);

        return ReservationDAO.getById(id);
    }

    public static Reservation checkIn(int id) throws SQLException {
        Reservation reservation = ReservationDAO.getById(id);
        if (reservation == null || reservation.status() != ReservationStatus.CONFIRMED) {
            return null;
        }

        ReservationDAO.updateStatus(id, ReservationStatus.CHECKED_IN);
        return ReservationDAO.getById(id);
    }

    public static Reservation checkOut(int id) throws SQLException {
        Reservation reservation = ReservationDAO.getById(id);
        if (reservation == null || reservation.status() != ReservationStatus.CHECKED_IN) {
            return null;
        }

        ReservationDAO.updateStatus(id, ReservationStatus.CHECKED_OUT);
        return ReservationDAO.getById(id);
    }

    public static Reservation cancelReservation(int id) throws SQLException {
        Reservation reservation = ReservationDAO.getById(id);
        if (reservation == null ||
            (reservation.status() != ReservationStatus.PENDING &&
             reservation.status() != ReservationStatus.CONFIRMED)) {
            return null;
        }

        BigDecimal refund = calculateRefund(reservation);
        ReservationDAO.updateStatus(id, ReservationStatus.CANCELLED);
        ReservationDAO.updateRefund(id, refund);

        return ReservationDAO.getById(id);
    }

    private static BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.status() == ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }

        if (reservation.ratePlan() == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }

        return reservation.amountPaid();
    }

    public static Reservation getDocument(int id) throws SQLException {
        return ReservationDAO.getById(id);
    }
}
