package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.infrastructure.MessageRepository;
import com.pousada.infrastructure.ReservationRepository;
import com.pousada.infrastructure.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;

    public ReservationService(RoomRepository roomRepository,
                            ReservationRepository reservationRepository,
                            MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                       GuestIdentification identification, LocalDate checkIn,
                                       LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(room.getCapacity(), guests);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(
            roomNumber, checkIn, checkOut);

        if (!conflicting.isEmpty()) {
            throw new RoomUnavailableException(roomNumber);
        }

        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, identification,
            checkIn, checkOut, guests, ratePlan, room.getNightlyRate()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private void sendConfirmationEmail(Reservation reservation) {
        DocumentGenerator generator = DocumentGenerator.forIdentification(reservation.getIdentification());
        String document = generator.generate(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }
}
