package com.pousadamarealta.service;

import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.notification.ReservationEventDispatcher;
import com.pousadamarealta.util.DateFormatter;
import com.pousadamarealta.util.Money;
import com.pousadamarealta.util.SeasonHelper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    public static Reservation createReservation(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) throws SQLException {

        Room room = RoomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (guests < 1 || guests > room.capacity()) {
            if (guests < 1) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        var conflicts = ReservationRepository.findConflicting(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = SeasonHelper.countNights(checkIn, checkOut);
        BigDecimal nightlyRate = room.nightlyRate();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal dailyRate = nightlyRate;
            if (SeasonHelper.isHighSeason(date)) {
                dailyRate = Money.multiply(nightlyRate, new BigDecimal("1.20"));
            }
            subtotal = Money.add(subtotal, dailyRate);
        }

        BigDecimal discount = ratePlan == RatePlan.NON_REFUNDABLE
            ? Money.percent(subtotal, 15)
            : BigDecimal.ZERO;

        BigDecimal total = Money.subtract(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
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

        long id = ReservationRepository.insert(reservation);
        reservation.id = id;
        return reservation;
    }

    public static Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = amountPaid;
        ReservationRepository.update(reservation);

        sendConfirmationEmail(reservation);
        ReservationEventDispatcher.fireOnConfirmed(reservation);

        return reservation;
    }

    public static Reservation checkIn(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        ReservationRepository.update(reservation);
        return reservation;
    }

    public static Reservation checkOut(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        ReservationRepository.update(reservation);
        return reservation;
    }

    public static Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation reservation = ReservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status == ReservationStatus.CHECKED_OUT ||
            reservation.status == ReservationStatus.CANCELLED ||
            reservation.status == ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        if (reservation.status == ReservationStatus.CONFIRMED) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                reservation.refund = reservation.amountPaid;
            } else {
                reservation.refund = BigDecimal.ZERO;
            }
        } else {
            reservation.refund = BigDecimal.ZERO;
        }

        reservation.status = ReservationStatus.CANCELLED;
        ReservationRepository.update(reservation);

        ReservationEventDispatcher.fireOnCancelled(reservation);

        return reservation;
    }

    public static Optional<Reservation> getReservation(long reservationId) throws SQLException {
        return ReservationRepository.findById(reservationId);
    }

    private static void sendConfirmationEmail(Reservation reservation) throws SQLException {
        String document = generateDocument(reservation);
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.guestName,
            document
        );

        MessageRepository.insert("EMAIL", reservation.guestEmail,
            "Reserva confirmada - Pousada Maré Alta", body);
    }

    public static String generateDocument(Reservation reservation) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.id));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.guestName, reservation.guestCpf));
        sb.append(String.format("Quarto: %d (%s)\n",
            reservation.roomNumber,
            getRoomType(reservation.roomNumber)));

        String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            DateFormatter.format(reservation.checkIn),
            DateFormatter.format(reservation.checkOut),
            nightsLabel));

        sb.append(String.format("Diárias: %s\n", DateFormatter.formatCurrency(reservation.subtotal)));

        if (reservation.discount.signum() > 0) {
            sb.append(String.format("Desconto: %s\n", DateFormatter.formatCurrency(reservation.discount)));
        }

        sb.append(String.format("Total: %s", DateFormatter.formatCurrency(reservation.total)));

        return sb.toString();
    }

    private static String getRoomType(int roomNumber) throws SQLException {
        return RoomRepository.findByNumber(roomNumber)
            .map(r -> r.type().toString())
            .orElse("UNKNOWN");
    }
}
