package com.pousadamarealta.service;

import com.pousadamarealta.document.Document;
import com.pousadamarealta.document.DocumentService;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final DocumentService documentService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             MessageRepository messageRepository,
                             DocumentService documentService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.documentService = documentService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        GuestIdentification guestIdentification, LocalDate checkIn,
                                        LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests > room.capacity()) {
            throw new CapacityExceededException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = new Reservation(0, roomNumber, guestName, guestEmail,
            guestIdentification, checkIn, checkOut, guests, ratePlan, room.nightlyRate());

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        Reservation saved = reservationRepository.save(reservation);
        sendConfirmationEmail(saved);
        return saved;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkInReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.performCheckIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Document checkOutReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.performCheckOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        reservationRepository.save(reservation);
        return generateDocument(reservation);
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.status() == ReservationStatus.PENDING ||
            reservation.status() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException();
        }

        return generateDocument(reservation);
    }

    private Document generateDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();
        return documentService.generateDocument(reservation, room.type());
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Document document = generateDocument(reservation);

        String body = "Olá, " + reservation.guestName() + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     document.text() + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        Message message = new Message("EMAIL", reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta", body);
        messageRepository.save(message);
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
