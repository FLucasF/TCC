package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.event.EventPublisher;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.repository.ReservationRepository;

import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomService roomService,
                             EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new RoomNotFoundException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("Invalid number of guests");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail,
            guestCpf, checkIn, checkOut, guests, ratePlan);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(ReservationNotFoundException::new);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException();
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        Money subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        Money discount = reservation.calculateDiscount(subtotal);
        Money total = reservation.calculateTotal(subtotal, discount);

        reservation.confirm(total);

        eventPublisher.publish(new ReservationConfirmedEvent(reservation, total));

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException();
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        Money refundAmount = reservation.cancel();

        eventPublisher.publish(new ReservationCancelledEvent(reservation, refundAmount, wasConfirmed));

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();
        return reservation;
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            if (!reservationRepository.findActiveByRoomAndNight(roomNumber, current).isEmpty()) {
                return false;
            }
            current = current.plusDays(1);
        }
        return true;
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
