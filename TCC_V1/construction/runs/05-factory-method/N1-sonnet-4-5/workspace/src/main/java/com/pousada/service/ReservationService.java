package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, GuestIdentification identification,
                                        String guestEmail, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest required");
        }

        LocalDate night = checkIn;
        while (night.isBefore(checkOut)) {
            List<Reservation> conflicting = reservationRepository.findByRoomAndNight(roomNumber, night);
            if (!conflicting.isEmpty()) {
                throw new RoomUnavailableException("Room already booked for selected dates");
            }
            night = night.plusDays(1);
        }

        long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(id, roomNumber, room.getType(), identification,
            guestEmail, checkIn, checkOut, guests, ratePlan);

        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(long id) {
        return reservationRepository.findById(id);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.confirm();
            messageService.sendConfirmationEmail(reservation);
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot confirm reservation in current status");
        }
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.cancel();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot cancel reservation in current status");
        }
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkIn();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot check in reservation in current status");
        }
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        try {
            reservation.checkOut();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException("Cannot check out reservation in current status");
        }
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
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

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
