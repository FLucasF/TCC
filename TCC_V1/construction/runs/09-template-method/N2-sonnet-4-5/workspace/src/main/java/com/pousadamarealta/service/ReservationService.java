package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        LocalDate night = checkIn;
        while (night.isBefore(checkOut)) {
            if (!reservationRepository.findActiveByRoomAndNight(roomNumber, night).isEmpty()) {
                throw new RoomUnavailableException("Room is not available for the selected dates");
            }
            night = night.plusDays(1);
        }

        Reservation reservation = new Reservation(
            reservationRepository.nextId(),
            roomNumber,
            room.getType(),
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.confirm();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot confirm this reservation");
        }
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.cancel();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot cancel this reservation");
        }
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkIn();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot check in this reservation");
        }
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkOut();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot check out this reservation");
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

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}
