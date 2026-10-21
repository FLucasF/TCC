package com.pousadamarealta.service;

import com.pousadamarealta.model.GuestIdentification;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final Repository repository;
    private final PricingService pricingService;
    private final DocumentGenerator documentGenerator;

    public ReservationService(Repository repository, PricingService pricingService, DocumentGenerator documentGenerator) {
        this.repository = repository;
        this.pricingService = pricingService;
        this.documentGenerator = documentGenerator;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        GuestIdentification identification,
                                        LocalDate checkIn, LocalDate checkOut,
                                        int guests, String ratePlan) throws ReservationException {
        Room room = repository.getRoom(roomNumber);
        if (room == null) {
            throw new ReservationException("ROOM_NOT_FOUND", "Room not found");
        }

        if (guests < 1 || guests > room.capacity) {
            throw new ReservationException("CAPACITY_EXCEEDED", "Guest count exceeds room capacity or is less than 1");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new ReservationException("INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, -1)) {
            throw new ReservationException("ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        int id = repository.getNextReservationId();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, identification,
                                                  checkIn, checkOut, guests, Reservation.RatePlan.valueOf(ratePlan),
                                                  nights, subtotal, discount, total);
        repository.saveReservation(reservation);
        return reservation;
    }

    public Reservation confirmReservation(int id, BigDecimal amountPaid) throws ReservationException {
        Reservation reservation = repository.getReservation(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.Status.PENDING) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservation.status = Reservation.Status.CONFIRMED;
        reservation.amountPaid = amountPaid;
        repository.saveReservation(reservation);

        Room room = repository.getRoom(reservation.roomNumber);
        var document = documentGenerator.generate(reservation, room);
        String emailBody = String.format("Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. " +
                "Estes são os dados dela:\n\n%s\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!", reservation.guestName, document.text);

        Message message = new Message(repository.getAllMessages().size() + 1, "EMAIL",
                                     reservation.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);
        repository.saveMessage(message);

        return reservation;
    }

    public Reservation checkIn(int id) throws ReservationException {
        Reservation reservation = repository.getReservation(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.Status.CONFIRMED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        reservation.status = Reservation.Status.CHECKED_IN;
        repository.saveReservation(reservation);
        return reservation;
    }

    public Reservation checkOut(int id) throws ReservationException {
        Reservation reservation = repository.getReservation(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.Status.CHECKED_IN) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        reservation.status = Reservation.Status.CHECKED_OUT;
        repository.saveReservation(reservation);
        return reservation;
    }

    public Reservation cancelReservation(int id) throws ReservationException {
        Reservation reservation = repository.getReservation(id);
        if (reservation == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.Status.PENDING && reservation.status != Reservation.Status.CONFIRMED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status");
        }

        if (reservation.ratePlan == Reservation.RatePlan.NON_REFUNDABLE) {
            reservation.refund = BigDecimal.ZERO;
        } else {
            reservation.refund = reservation.amountPaid;
        }

        reservation.status = Reservation.Status.CANCELLED;
        repository.saveReservation(reservation);
        return reservation;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, int excludeReservationId) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            for (Reservation res : repository.getAllReservations()) {
                if (res.id != excludeReservationId && res.roomNumber == roomNumber &&
                    res.status != Reservation.Status.CANCELLED) {
                    if (!current.isBefore(res.checkIn) && current.isBefore(res.checkOut)) {
                        return true;
                    }
                }
            }
            current = current.plusDays(1);
        }
        return false;
    }

    public static class ReservationException extends Exception {
        public final String code;

        public ReservationException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
