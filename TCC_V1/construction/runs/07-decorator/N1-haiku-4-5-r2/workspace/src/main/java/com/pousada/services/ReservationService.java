package com.pousada.services;

import com.pousada.db.Database;
import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.utils.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final Database db;
    private final MessageService messageService;

    public ReservationService(Database db, MessageService messageService) {
        this.db = db;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) {
        Room room = findRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            if (guests > room.getCapacity()) {
                throw new IllegalArgumentException("CAPACITY_EXCEEDED");
            } else {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (db.isRoomOccupied(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        PriceCalculator.PriceBreakdown prices = PriceCalculator.calculate(
            room.getNightlyRate(), checkIn, checkOut, ratePlan);

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            Reservation.RatePlan.valueOf(ratePlan),
            nights,
            prices.subtotal,
            prices.discount,
            prices.total,
            Reservation.Status.PENDING,
            BigDecimal.ZERO,
            null
        );

        return db.saveReservation(reservation);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = db.getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Reservation confirmed = reservation
            .withStatus(Reservation.Status.CONFIRMED)
            .withAmountPaid(amountPaid);

        confirmed = db.updateReservation(confirmed);

        Room room = findRoom(reservation.getRoomNumber());
        String roomTypeName = room.getType().name();

        Receipt receipt = new Receipt(
            confirmed.getId(),
            confirmed.getGuestName(),
            confirmed.getGuestCpf(),
            confirmed.getRoomNumber(),
            roomTypeName,
            confirmed.getCheckIn(),
            confirmed.getCheckOut(),
            confirmed.getNights(),
            confirmed.getSubtotal(),
            confirmed.getDiscount(),
            confirmed.getTotal()
        );

        String receiptText = receipt.format();
        String emailSubject = "Reserva confirmada - Pousada Maré Alta";
        String emailBody = "Olá, " + confirmed.getGuestName() + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            receiptText + "\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";

        messageService.sendMessage(confirmed.getGuestEmail(), emailSubject, emailBody);

        return confirmed;
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = db.getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        return db.updateReservation(reservation.withStatus(Reservation.Status.CHECKED_IN));
    }

    public Receipt checkOut(long reservationId) {
        Reservation reservation = db.getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation = db.updateReservation(reservation.withStatus(Reservation.Status.CHECKED_OUT));

        Room room = findRoom(reservation.getRoomNumber());
        String roomTypeName = room.getType().name();

        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomTypeName,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public Receipt getDocument(long reservationId) {
        Reservation reservation = db.getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED &&
            reservation.getStatus() != Reservation.Status.CHECKED_IN &&
            reservation.getStatus() != Reservation.Status.CHECKED_OUT) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Room room = findRoom(reservation.getRoomNumber());
        String roomTypeName = room.getType().name();

        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            roomTypeName,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = db.getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING &&
            reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
            if (reservation.getRatePlan() == Reservation.RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        return db.updateReservation(
            reservation.withStatus(Reservation.Status.CANCELLED).withRefund(refund)
        );
    }

    public Reservation getReservation(long id) {
        return db.getReservation(id);
    }

    private Room findRoom(int number) {
        for (Room room : Room.FIXED_ROOMS) {
            if (room.getNumber() == number) {
                return room;
            }
        }
        return null;
    }
}
