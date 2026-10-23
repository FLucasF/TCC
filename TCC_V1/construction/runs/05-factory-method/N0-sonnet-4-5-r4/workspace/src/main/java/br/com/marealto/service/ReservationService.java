package br.com.marealto.service;

import br.com.marealto.model.*;
import br.com.marealto.repository.MessageRepository;
import br.com.marealto.repository.ReservationRepository;
import br.com.marealto.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;

    public ReservationService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            MessageRepository messageRepository,
            PricingService pricingService,
            DocumentService documentService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!reservation.getCheckOut().isAfter(reservation.getCheckIn())) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal subtotal = pricingService.calculateSubtotal(room, reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal discount = pricingService.calculateDiscount(subtotal, reservation.getRatePlan());
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm pending reservations");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Cannot cancel reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD &&
            reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check in confirmed reservations");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check out guests who checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String emailBody = documentService.generateConfirmationEmail(reservation, room.getType());

        Message message = new Message(
                "EMAIL",
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                emailBody
        );

        messageRepository.save(message);
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

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
