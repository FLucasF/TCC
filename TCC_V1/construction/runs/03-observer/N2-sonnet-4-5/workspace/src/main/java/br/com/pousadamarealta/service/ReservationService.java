package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.repository.ReservationRepository;

import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository, NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(String roomNumber, RoomType roomType, String guestName,
                                        String guestEmail, String guestCpf, java.time.LocalDate checkIn,
                                        java.time.LocalDate checkOut, int guests, RatePlan ratePlan) {

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        if (guests > roomType.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }

        Reservation reservation = new Reservation(roomNumber, roomType, guestName, guestEmail,
                                                  guestCpf, checkIn, checkOut, guests, ratePlan);
        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(Long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.confirm();
        reservationRepository.save(reservation);

        notificationService.notifyConfirmation(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        reservation.cancel();
        reservationRepository.save(reservation);

        notificationService.notifyCancellation(reservation, wasConfirmed);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.checkIn();
        reservationRepository.save(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.checkOut();
        reservationRepository.save(reservation);

        return reservation;
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

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
