package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessagingService messagingService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessagingService messagingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messagingService = messagingService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = Reservation.createNew(
            roomNumber, room.getType(), room.getNightlyRate(),
            guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findById(id);
        reservation.confirm();
        reservationRepository.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = findById(id);
        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);
        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);
        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public ReservationDocument getDocument(Long id) {
        Reservation reservation = findById(id);
        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }
        return new ReservationDocument(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        ReservationDocument document = new ReservationDocument(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation, document);

        messagingService.send(reservation.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmailBody(Reservation reservation, ReservationDocument document) {
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            document.formatAsText()
        );
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
