package com.pousada.marealta.services;

import com.pousada.marealta.db.Database;
import com.pousada.marealta.models.Receipt;
import com.pousada.marealta.models.Reservation;
import com.pousada.marealta.models.ReservationStatus;
import com.pousada.marealta.models.Room;
import com.pousada.marealta.util.PricingUtil;
import com.pousada.marealta.util.ReceiptFormatter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    private final Database database;
    private final EmailService emailService;

    public ReservationService(Database database, EmailService emailService) {
        this.database = database;
        this.emailService = emailService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) throws SQLException {
        Room room = database.getRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.capacity) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!database.isRoomAvailable(roomNumber, checkIn, checkOut, -1)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = PricingUtil.countNights(checkIn, checkOut);
        BigDecimal subtotal = PricingUtil.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = PricingUtil.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingUtil.calculateTotal(subtotal, discount);

        long reservationId = database.createReservation(roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        return database.getReservation(reservationId);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws SQLException {
        Reservation res = database.getReservation(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.status != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationStatus(reservationId, ReservationStatus.CONFIRMED, amountPaid, null);
        Reservation confirmed = database.getReservation(reservationId);

        Room room = database.getRoom(res.roomNumber);
        Receipt receipt = new Receipt(reservationId, res.guestName, res.guestCpf, res.roomNumber,
                room.type, res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total);
        String receiptText = ReceiptFormatter.format(receipt);

        emailService.sendConfirmationEmail(res.guestName, res.guestEmail, receiptText);

        return confirmed;
    }

    public Reservation checkIn(long reservationId) throws SQLException {
        Reservation res = database.getReservation(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationStatus(reservationId, ReservationStatus.CHECKED_IN, res.amountPaid, res.refund);
        return database.getReservation(reservationId);
    }

    public Receipt checkOut(long reservationId) throws SQLException {
        Reservation res = database.getReservation(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.status != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        database.updateReservationStatus(reservationId, ReservationStatus.CHECKED_OUT, res.amountPaid, res.refund);
        Reservation updated = database.getReservation(reservationId);

        Room room = database.getRoom(res.roomNumber);
        return new Receipt(reservationId, res.guestName, res.guestCpf, res.roomNumber,
                room.type, res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total);
    }

    public Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation res = database.getReservation(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.status != ReservationStatus.PENDING && res.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (res.status == ReservationStatus.CONFIRMED) {
            if ("STANDARD".equals(res.ratePlan)) {
                refund = res.amountPaid;
            }
        }

        database.updateReservationStatus(reservationId, ReservationStatus.CANCELLED, res.amountPaid, refund);
        return database.getReservation(reservationId);
    }

    public Receipt getReceipt(long reservationId) throws SQLException {
        Reservation res = database.getReservation(reservationId);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (res.status != ReservationStatus.CONFIRMED && res.status != ReservationStatus.CHECKED_IN && res.status != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Room room = database.getRoom(res.roomNumber);
        return new Receipt(reservationId, res.guestName, res.guestCpf, res.roomNumber,
                room.type, res.checkIn, res.checkOut, res.nights, res.subtotal, res.discount, res.total);
    }
}
