package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.messaging.MessageChannel;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final PriceCalculator priceCalculator;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                             MessageService messageService, PriceCalculator priceCalculator) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.priceCalculator = priceCalculator;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests > room.capacity()) {
            throw new CapacityExceededException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        List<Reservation> conflicts = reservationRepository.findActiveByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = Reservation.create(roomNumber, room.type(), guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, priceCalculator);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        Reservation updated = reservationRepository.save(reservation);
        sendConfirmationEmail(updated);
        return updated;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.performCheckIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.performCheckOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException();
        }

        return reservationRepository.save(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.roomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found"));
        String receipt = DocumentFormatter.formatReceipt(reservation, room);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.guestName(), receipt);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            reservation.guestEmail(),
            subject,
            body
        );

        messageService.send(message);
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomNumber) {
            super("Room not found: " + roomNumber);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reservation not found: " + id);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
