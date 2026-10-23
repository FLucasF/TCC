package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        PricingService pricingService,
        MessageService messageService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(
        String roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.capacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservationRepository.hasConflictingReservation(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.nightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirm(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        ReservationDocument document = createDocument(reservation);
        messageService.sendReservationConfirmation(reservation, document);

        return reservation;
    }

    public Reservation cancel(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(refundAmount);
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.save(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.save(reservation);
        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);
        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public ReservationDocument createDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException());

        return new ReservationDocument(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.type(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
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
