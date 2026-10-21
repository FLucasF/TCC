package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final EmailService emailService;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                            PricingService pricingService, EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.emailService = emailService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan, String companyCode) {
        validateReservationRequest(roomNumber, checkIn, checkOut, guests, ratePlan, companyCode);

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        int nights = (int) checkIn.until(checkOut).getDays();
        PricingService.PriceBreakdown price = pricingService.calculatePrice(room, checkIn, checkOut, ratePlan, companyCode);

        Long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(
                id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, companyCode,
                nights, price.subtotal, price.discount, price.total
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm pending reservations");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        emailService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = findReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only cancel pending or confirmed reservations");
        }

        BigDecimal refundAmount = pricingService.calculateRefund(
                reservation.getAmountPaid(), reservation.getRatePlan());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check in confirmed reservations");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check out guests who have checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    private void validateReservationRequest(String roomNumber, LocalDate checkIn, LocalDate checkOut,
                                          int guests, RatePlan ratePlan, String companyCode) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidRequestException("Check-in and check-out dates are required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        try {
            pricingService.validateRatePlan(ratePlan, checkIn, checkOut, companyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
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
