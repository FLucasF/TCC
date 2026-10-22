package com.pousada.marealta.service;

import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.model.*;
import com.pousada.marealta.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) throws Exception {
        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.capacity) {
            throw new IllegalArgumentException(guests > room.capacity ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyPrice = PriceCalculator.calculateNightlyPrice(room.nightlyRate, date);
            subtotal = subtotal.add(nightlyPrice);
        }
        subtotal = PriceCalculator.roundPrice(subtotal);

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan.name());
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        return ReservationRepository.create(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests, ratePlan, nights, subtotal, discount, total);
    }

    public static Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.status != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        ReservationRepository.updateStatusAndAmountPaid(reservationId, ReservationStatus.CONFIRMED, amountPaid);

        sendConfirmationEmail(reservation);

        return ReservationRepository.findById(reservationId);
    }

    public static Reservation checkIn(long reservationId) throws Exception {
        Reservation reservation = ReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.status != ReservationStatus.CONFIRMED) {
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

        if (reservation.status != ReservationStatus.CHECKED_IN) {
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

        if (reservation.status != ReservationStatus.PENDING && reservation.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund;
        if (reservation.ratePlan == RatePlan.STANDARD && reservation.status == ReservationStatus.CONFIRMED) {
            refund = reservation.amountPaid;
        } else {
            refund = BigDecimal.ZERO;
        }

        ReservationRepository.updateStatusAndRefund(reservationId, ReservationStatus.CANCELLED, refund);
        return ReservationRepository.findById(reservationId);
    }

    private static void sendConfirmationEmail(Reservation reservation) throws Exception {
        Room room = RoomRepository.findByNumber(reservation.roomNumber);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation, room);
        MessageService.sendMessage(reservation.guestEmail, subject, body);
    }

    private static String buildConfirmationEmailBody(Reservation reservation, Room room) {
        String document = DocumentService.buildDocument(reservation, room);
        return String.format("Olá, %s!\n\n" +
                        "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                        "%s\n\n" +
                        "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                        "Até breve!",
                reservation.guestName, document);
    }
}
