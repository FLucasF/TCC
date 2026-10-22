package com.pousadamarealta.service;

import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.util.PricingCalculator;
import com.pousadamarealta.util.ReceiptFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final MessagingService messagingService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            MessagingService messagingService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.messagingService = messagingService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {

        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!reservationRepository.findConflictingReservations(roomNumber, checkIn, checkOut).isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        PricingCalculator.PricingResult pricing = PricingCalculator.calculate(room, checkIn, checkOut, ratePlan);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(pricing.getNights());
        reservation.setSubtotal(pricing.getSubtotal());
        reservation.setDiscount(pricing.getDiscount());
        reservation.setTotal(pricing.getTotal());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException("Cannot cancel after check-in");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Reservation is already cancelled");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public String getReceipt(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Receipt is only available for paid reservations");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        return ReceiptFormatter.formatReceipt(reservation, room);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        String receipt = ReceiptFormatter.formatReceipt(reservation, room);
        String emailBody = ReceiptFormatter.formatConfirmationEmail(reservation.getGuestName(), receipt);

        messagingService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
