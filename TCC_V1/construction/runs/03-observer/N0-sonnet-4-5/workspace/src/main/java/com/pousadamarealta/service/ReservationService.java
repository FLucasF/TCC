package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.events.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final EventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            PricingService pricingService,
            EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.eventPublisher = eventPublisher;
    }

    public Reservation createReservation(
            String roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) {

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
                roomNumber, checkIn, checkOut, null);

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationConfirmedEvent(reservation));

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Reservation cannot be cancelled");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (wasConfirmed && reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }
        reservation.setRefund(refundAmount);

        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationCancelledEvent(reservation, wasConfirmed));

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
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
}
