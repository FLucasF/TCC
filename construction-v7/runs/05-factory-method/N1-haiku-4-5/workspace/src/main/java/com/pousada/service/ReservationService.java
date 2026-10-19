package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.InMemoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final InMemoryRepository repository = InMemoryRepository.getInstance();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         GuestIdentification guestIdentification, LocalDate checkIn,
                                         LocalDate checkOut, int guests, Reservation.RatePlan ratePlan) {
        Room room = repository.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new ValidationException("ROOM_NOT_FOUND", "Room not found");
        }

        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Room capacity exceeded");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ValidationException("INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (!repository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        }

        long nights = PricingService.getNights(checkIn, checkOut);
        BigDecimal subtotal = PricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(0, roomNumber, guestName, guestEmail,
            guestIdentification, checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total);

        long id = repository.saveReservation(reservation);
        return repository.getReservation(id);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = repository.getReservation(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Can only confirm a PENDING reservation");
        }

        reservation.confirm(amountPaid);

        Room room = repository.getRoomByNumber(reservation.getRoomNumber());
        Document document = DocumentGenerator.generateDocument(reservation, room);

        String emailBody = generateConfirmationEmail(reservation, document);
        repository.saveMessage("EMAIL", reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta", emailBody);

        return reservation;
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = repository.getReservation(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Can only check in a CONFIRMED reservation");
        }

        reservation.checkIn();
        return reservation;
    }

    public Document checkOut(long reservationId) {
        Reservation reservation = repository.getReservation(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Can only check out a CHECKED_IN reservation");
        }

        reservation.checkOut();
        Room room = repository.getRoomByNumber(reservation.getRoomNumber());
        return DocumentGenerator.generateDocument(reservation, room);
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = repository.getReservation(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING &&
            reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Can only cancel PENDING or CONFIRMED reservations");
        }

        reservation.cancel();
        return reservation;
    }

    public Document getDocument(long reservationId) {
        Reservation reservation = repository.getReservation(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED &&
            reservation.getStatus() != Reservation.Status.CHECKED_IN &&
            reservation.getStatus() != Reservation.Status.CHECKED_OUT) {
            throw new ValidationException("OPERATION_NOT_ALLOWED",
                "Document is only available for paid reservations");
        }

        Room room = repository.getRoomByNumber(reservation.getRoomNumber());
        return DocumentGenerator.generateDocument(reservation, room);
    }

    private String generateConfirmationEmail(Reservation reservation, Document document) {
        return String.format(
            "Olá, %s!%n%n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:%n%n" +
            "%s%n%n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.%n" +
            "Até breve!",
            reservation.getGuestName(),
            document.getText()
        );
    }

    public static class ValidationException extends RuntimeException {
        private final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
