package com.marealta.domain.service;

import com.marealta.domain.event.ReservationCancelledEvent;
import com.marealta.domain.event.ReservationConfirmedEvent;
import com.marealta.domain.model.RatePlan;
import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;
import com.marealta.domain.model.Room;
import com.marealta.domain.repository.ReservationRepository;
import com.marealta.domain.repository.RoomRepository;
import com.marealta.infrastructure.notification.EventDispatcher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final EventDispatcher eventDispatcher;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            PricingService pricingService,
                            EventDispatcher eventDispatcher) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.eventDispatcher = eventDispatcher;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(room.getCapacity(), guests);
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException(roomNumber, checkIn, checkOut);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
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

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        Reservation updated = reservationRepository.save(reservation);

        eventDispatcher.dispatch(new ReservationConfirmedEvent(updated));

        return updated;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check in reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check out reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        ReservationStatus previousStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refund = BigDecimal.ZERO;
        if (previousStatus == ReservationStatus.CONFIRMED && reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }
        reservation.setRefund(refund);

        Reservation updated = reservationRepository.save(reservation);

        eventDispatcher.dispatch(new ReservationCancelledEvent(updated, previousStatus));

        return updated;
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomNumber) {
            super("Room not found: " + roomNumber);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reservation not found: " + id);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        private final int capacity;
        private final int guests;

        public CapacityExceededException(int capacity, int guests) {
            super("Room capacity exceeded: " + guests + " guests for capacity " + capacity);
            this.capacity = capacity;
            this.guests = guests;
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
            super("Room " + roomNumber + " is not available from " + checkIn + " to " + checkOut);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
