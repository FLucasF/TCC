package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final MessageService messageService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            PricingService pricingService,
            DocumentService documentService,
            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.documentService = documentService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException());

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().equals(reservation.getCheckIn())) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, reservation.getRatePlan());
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        Document document = documentService.createDocument(reservation, room);
        String receipt = documentService.formatDocument(document);
        String emailBody = messageService.createConfirmationEmail(reservation.getGuestName(), receipt);

        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD &&
            reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public static class RoomNotFoundException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class OperationNotAllowedException extends RuntimeException {
    }
}
