package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.infrastructure.ReservationRepository;
import com.pousadamarealta.infrastructure.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        validateRoom(roomNumber);
        validateDates(checkIn, checkOut);
        validateGuests(roomNumber, guests);
        validateRoomAvailability(roomNumber, checkIn, checkOut, null);

        Reservation reservation = Reservation.create(roomNumber, guestName, guestEmail, guestCpf,
                                                     checkIn, checkOut, guests, ratePlan);
        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findReservation(id);
        reservation.confirm();
        reservationRepository.save(reservation);
        notificationService.sendConfirmationEmail(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findReservation(id);
        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findReservation(id);
        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = findReservation(id);
        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    public Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private void validateRoom(String roomNumber) {
        roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException(roomNumber));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("A data de saída deve ser posterior à data de entrada");
        }
    }

    private void validateGuests(String roomNumber, int guests) {
        if (guests < 1) {
            throw new InvalidRequestException("O número de hóspedes deve ser no mínimo 1");
        }

        Room room = roomRepository.findByNumber(roomNumber).orElseThrow();
        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(roomNumber, room.getCapacity(), guests);
        }
    }

    private void validateRoomAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, excludeId);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException(roomNumber, checkIn, checkOut);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(Long id) {
            super("Reserva não encontrada: " + id);
        }
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomNumber) {
            super("Quarto não encontrado: " + roomNumber);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String roomNumber, int capacity, int guests) {
            super(String.format("Quarto %s comporta no máximo %d hóspedes, mas foram solicitados %d",
                              roomNumber, capacity, guests));
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
            super(String.format("Quarto %s não está disponível no período de %s a %s",
                              roomNumber, checkIn, checkOut));
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
