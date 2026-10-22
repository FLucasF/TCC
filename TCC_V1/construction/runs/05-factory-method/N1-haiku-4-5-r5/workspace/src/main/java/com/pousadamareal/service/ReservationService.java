package com.pousadamareal.service;

import com.pousadamareal.model.*;
import com.pousadamareal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public static Reservation create(int roomNumber, String guestName, String guestEmail,
                                     String guestCpf, String companyCnpj, String companyName,
                                     String guestPassport, String guestCountry,
                                     LocalDate checkIn, LocalDate checkOut,
                                     int guests, RatePlan ratePlan) throws SQLException {

        Optional<Room> room = RoomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.get().capacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        List<Reservation> conflicts = ReservationRepository.findByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = PricingUtils.calculateNightlyRate(room.get().nightlyRate(), date);
            subtotal = subtotal.add(nightlyRate);
        }
        subtotal = subtotal.setScale(2, java.math.RoundingMode.HALF_EVEN);

        BigDecimal discount = PricingUtils.calculateDiscount(subtotal, ratePlan.name());
        BigDecimal total = PricingUtils.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return ReservationRepository.save(reservation);
    }

    public static Reservation getById(long id) throws SQLException {
        Optional<Reservation> reservation = ReservationRepository.findById(id);
        if (reservation.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservation.get();
    }

    public static Reservation confirm(long id, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = getById(id);

        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Reservation updated = reservation
            .withStatus(ReservationStatus.CONFIRMED)
            .withAmountPaid(amountPaid);

        ReservationRepository.update(updated);

        Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber());
        Document document = DocumentGenerator.generate(updated, room.get());

        sendConfirmationEmail(updated, document);

        return updated;
    }

    public static Reservation cancel(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (reservation.status() == ReservationStatus.CHECKED_IN ||
            reservation.status() == ReservationStatus.CHECKED_OUT ||
            reservation.status() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            if (reservation.ratePlan() == RatePlan.STANDARD) {
                refund = reservation.amountPaid();
            }
        }

        Reservation updated = reservation
            .withStatus(ReservationStatus.CANCELLED)
            .withRefund(refund);

        ReservationRepository.update(updated);
        return updated;
    }

    public static Reservation checkIn(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Reservation updated = reservation.withStatus(ReservationStatus.CHECKED_IN);
        ReservationRepository.update(updated);
        return updated;
    }

    public static Reservation checkOut(long id) throws SQLException {
        Reservation reservation = getById(id);

        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Reservation updated = reservation.withStatus(ReservationStatus.CHECKED_OUT);
        ReservationRepository.update(updated);
        return updated;
    }

    private static void sendConfirmationEmail(Reservation reservation, Document document) throws SQLException {
        String emailBody = buildConfirmationEmailBody(reservation.guestName(), document.text());

        Message message = new Message(
            0,
            "EMAIL",
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        MessageRepository.save(message);
    }

    private static String buildConfirmationEmailBody(String guestName, String documentText) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               documentText + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }
}
