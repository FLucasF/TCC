package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PriceCalculator priceCalculator;
    private final MessageService messageService;

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
        ReservationStatus.PENDING, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED),
        ReservationStatus.CONFIRMED, Set.of(ReservationStatus.CHECKED_IN, ReservationStatus.CANCELLED),
        ReservationStatus.CHECKED_IN, Set.of(ReservationStatus.CHECKED_OUT)
    );

    public ReservationService(ReservationRepository reservationRepository,
                              RoomService roomService,
                              PriceCalculator priceCalculator,
                              MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.priceCalculator = priceCalculator;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException();
        }

        if (guests < 1) {
            throw new InvalidRequestException();
        }

        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = priceCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = priceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = priceCalculator.calculateTotal(subtotal, discount);

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

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!canTransition(reservation.getStatus(), ReservationStatus.CONFIRMED)) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (!canTransition(reservation.getStatus(), ReservationStatus.CHECKED_IN)) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (!canTransition(reservation.getStatus(), ReservationStatus.CHECKED_OUT)) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!canTransition(reservation.getStatus(), ReservationStatus.CANCELLED)) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refund = calculateRefund(reservation);
        reservation.setRefund(refund);

        reservationRepository.update(reservation);

        return reservation;
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            return reservation.getAmountPaid();
        }
        return BigDecimal.ZERO;
    }

    private boolean canTransition(ReservationStatus from, ReservationStatus to) {
        Set<ReservationStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    public static class InvalidRequestException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class OperationNotAllowedException extends RuntimeException {
    }
}
