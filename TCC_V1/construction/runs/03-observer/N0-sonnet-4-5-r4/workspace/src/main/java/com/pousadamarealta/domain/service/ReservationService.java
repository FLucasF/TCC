package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.event.EventBus;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.ReservationStatus;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final EventBus eventBus;

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService, EventBus eventBus) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.eventBus = eventBus;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        if (room == null) {
            throw new RoomNotFoundException();
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        try {
            if (reservationRepository.hasConflict(
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                null
            )) {
                throw new RoomUnavailableException();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(
            room.getNightlyRate(),
            reservation.getCheckIn(),
            reservation.getCheckOut()
        );
        reservation.setSubtotal(subtotal);

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, reservation.getRatePlan());
        reservation.setDiscount(discount);

        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        try {
            return reservationRepository.save(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    public Reservation getReservation(Long id) {
        try {
            Reservation reservation = reservationRepository.findById(id);
            if (reservation == null) {
                throw new ReservationNotFoundException();
            }
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve reservation", e);
        }
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        try {
            reservationRepository.update(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        eventBus.publish(new ReservationConfirmedEvent(reservation));

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        BigDecimal refund = BigDecimal.ZERO;
        if (wasConfirmed && reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        try {
            reservationRepository.update(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        eventBus.publish(new ReservationCancelledEvent(reservation, wasConfirmed));

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);

        try {
            reservationRepository.update(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        try {
            reservationRepository.update(reservation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }

        return reservation;
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}
