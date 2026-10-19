package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final DocumentService documentService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageService messageService,
                            DocumentService documentService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.documentService = documentService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlanType ratePlanType) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room not available for selected dates");
        }

        long id = reservationRepository.getNextId();
        Reservation reservation = new Reservation(id, roomNumber, room.getType(), guestName,
                                                  guestEmail, guestCpf, checkIn, checkOut,
                                                  guests, ratePlanType);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.confirm();
            reservationRepository.save(reservation);

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            String receipt = documentService.generateReceipt(reservation, room);
            String emailBody = documentService.generateConfirmationEmail(reservation, receipt);

            messageService.sendEmail(
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody
            );

            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.cancel();
            return reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkIn();
            return reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkOut();
            return reservationRepository.save(reservation);
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Optional<Reservation> findById(long id) {
        return reservationRepository.findById(id);
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
