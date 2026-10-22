package com.pousadamaralta.domain.service;

import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.ReservationStatus;
import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.domain.model.RatePlanType;
import com.pousadamaralta.infrastructure.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomService roomService,
            PricingService pricingService,
            MessageService messageService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(
            String roomNumber,
            String guestName,
            String guestEmail,
            GuestIdentification identification,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlanType ratePlanType
    ) {
        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(guests, room.getCapacity());
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(
            roomNumber, checkIn, checkOut
        );

        if (!conflicting.isEmpty()) {
            throw new RoomUnavailableException(roomNumber);
        }

        int nights = pricingService.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(ratePlanType, subtotal);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            null,
            roomNumber,
            guestName,
            guestEmail,
            identification,
            checkIn,
            checkOut,
            guests,
            ratePlanType,
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

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException(
                "Cannot confirm reservation in status " + reservation.getStatus()
            );
        }

        reservation.confirm();
        reservation = reservationRepository.save(reservation);

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        messageService.sendConfirmationEmail(reservation, room);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException(
                "Cannot cancel reservation in status " + reservation.getStatus()
            );
        }

        BigDecimal refund = pricingService.calculateRefund(
            reservation.getRatePlanType(),
            reservation.getAmountPaid(),
            reservation.getTotal()
        );

        reservation.cancel(refund);
        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException(
                "Cannot check in reservation in status " + reservation.getStatus()
            );
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException(
                "Cannot check out reservation in status " + reservation.getStatus()
            );
        }

        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reservation not found: " + id);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String roomNumber) {
            super("Room " + roomNumber + " is not available for the selected dates");
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(int requested, int capacity) {
            super("Requested " + requested + " guests but room capacity is " + capacity);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
