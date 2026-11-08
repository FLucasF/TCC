package com.pousadamarealta.service;

import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.PriceCalculator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room;
        try {
            room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve room", e);
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts;
        try {
            conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

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

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = checkIn;
        for (int i = 0; i < nights; i++) {
            BigDecimal nightPrice = PriceCalculator.calculateNightPrice(room.getNightlyRate(), currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }
        subtotal = PriceCalculator.roundMoney(subtotal);
        reservation.setSubtotal(subtotal);

        boolean isNonRefundable = ratePlan == RatePlan.NON_REFUNDABLE;
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, isNonRefundable);
        reservation.setDiscount(discount);

        BigDecimal total = PriceCalculator.roundMoney(subtotal.subtract(discount));
        reservation.setTotal(total);

        try {
            return reservationRepository.save(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservation", e);
        }
    }

    public Reservation getReservation(Long id) {
        try {
            return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve reservation", e);
        }
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        try {
            reservation = reservationRepository.save(reservation);
            eventPublisher.publish(new ReservationConfirmedEvent(reservation));
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to confirm reservation", e);
        }
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException("Cannot cancel reservation after check-in");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Reservation is already cancelled");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        BigDecimal refund = BigDecimal.ZERO;
        if (wasConfirmed && reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        try {
            reservation = reservationRepository.save(reservation);
            eventPublisher.publish(new ReservationCancelledEvent(reservation, wasConfirmed));
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel reservation", e);
        }
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);

        try {
            return reservationRepository.save(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check in", e);
        }
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        try {
            return reservationRepository.save(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check out", e);
        }
    }

    public List<Reservation> getReservationsByDate(LocalDate date) {
        try {
            return reservationRepository.findByDate(date);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve reservations", e);
        }
    }

    public List<Reservation> getReservationsByMonth(int year, int month) {
        try {
            return reservationRepository.findByMonth(year, month);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve reservations", e);
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
