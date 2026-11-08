package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.pricing.PricingCalculator;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;
    private final PricingCalculator pricingCalculator;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
        this.pricingCalculator = new PricingCalculator();
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
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

        BigDecimal subtotal = pricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingCalculator.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = pricingCalculator.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.confirm();
        reservationRepository.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.cancel();
        reservationRepository.update(reservation);

        return reservation;
    }

    public static class RoomNotFoundException extends RuntimeException {
        public String getErrorCode() { return "ROOM_NOT_FOUND"; }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public String getErrorCode() { return "RESERVATION_NOT_FOUND"; }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) { super(message); }
        public String getErrorCode() { return "INVALID_REQUEST"; }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public String getErrorCode() { return "ROOM_UNAVAILABLE"; }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public String getErrorCode() { return "OPERATION_NOT_ALLOWED"; }
    }

    public static class CapacityExceededException extends RuntimeException {
        public String getErrorCode() { return "CAPACITY_EXCEEDED"; }
    }
}
