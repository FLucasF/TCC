package com.pousadamareal.service;

import com.pousadamareal.model.*;
import com.pousadamareal.repository.ReservationRepository;
import com.pousadamareal.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            PricingService pricingService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        GuestIdentification identification,
                                        LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
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

        if (!reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null).isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0, roomNumber, guestName, guestEmail, identification,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        Document document = reservation.generateDocument(room.getType());
        messageService.sendConfirmationEmail(reservation.getGuestName(), reservation.getGuestEmail(), document);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Document checkOut(long id) {
        Reservation reservation = getReservation(id);

        try {
            reservation.checkOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }

        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return reservation.generateDocument(room.getType());
    }

    public Document getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return reservation.generateDocument(room.getType());
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
