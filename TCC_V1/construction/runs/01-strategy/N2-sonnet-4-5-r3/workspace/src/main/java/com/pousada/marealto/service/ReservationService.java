package com.pousada.marealto.service;

import com.pousada.marealto.domain.Reservation;
import com.pousada.marealto.domain.Room;
import com.pousada.marealto.domain.ReservationStatus;
import com.pousada.marealto.rateplan.*;
import com.pousada.marealto.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Reservation reservation) {
        reservation.calculateNights();

        if (reservation.getNights() < 1) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservation.getGuests() < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        RatePlanStrategy strategy = RatePlanFactory.getStrategy(reservation.getRatePlan());

        try {
            strategy.validate(reservation.getCheckIn(), reservation.getCheckOut(),
                            reservation.getNights(), reservation.getCompanyCode());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        if (reservationRepository.hasConflictingReservation(reservation.getRoomNumber(),
                reservation.getCheckIn(), reservation.getCheckOut(), null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        PriceCalculation price;
        if (strategy instanceof CorporateRatePlan corporate) {
            price = corporate.calculatePrice(room.getType(), reservation.getCheckIn(),
                                           reservation.getCheckOut(), reservation.getNights(),
                                           reservation.getCompanyCode());
        } else {
            price = strategy.calculatePrice(room.getType(), reservation.getCheckIn(),
                                          reservation.getCheckOut(), reservation.getNights());
        }

        reservation.setSubtotal(price.subtotal());
        reservation.setDiscount(price.discount());
        reservation.setTotal(price.total());

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm pending reservations");
        }

        reservation.confirm();
        reservationRepository.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only cancel pending or confirmed reservations");
        }

        RatePlanStrategy strategy = RatePlanFactory.getStrategy(reservation.getRatePlan());
        BigDecimal refund = strategy.calculateRefund(reservation.getAmountPaid());

        reservation.cancel();
        reservation.setRefund(refund);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check-in confirmed reservations");
        }

        reservation.checkIn();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check-out checked-in reservations");
        }

        reservation.checkOut();
        reservationRepository.update(reservation);

        return reservation;
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

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }
}
