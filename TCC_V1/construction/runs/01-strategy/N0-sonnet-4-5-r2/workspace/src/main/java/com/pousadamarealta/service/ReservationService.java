package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             MessageRepository messageRepository,
                             PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        validateReservationRequest(reservation, room);

        pricingService.calculatePricing(reservation, room);

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        return reservationRepository.save(reservation);
    }

    private void validateReservationRequest(Reservation reservation, Room room) {
        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().equals(reservation.getCheckIn())) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.save(reservation);
        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        try {
            var refund = pricingService.calculateRefund(reservation);
            reservation.cancel(refund);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        return reservationRepository.save(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String receipt = DocumentService.generateReceipt(reservation);
        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        messageRepository.save(message);
    }
}
