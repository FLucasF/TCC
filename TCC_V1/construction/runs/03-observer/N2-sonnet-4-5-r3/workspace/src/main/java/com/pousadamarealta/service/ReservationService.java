package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.ReservationEventListener;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public final class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final List<ReservationEventListener> listeners;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        List<ReservationEventListener> listeners
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation createReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Guests must be at least 1");
        }

        if (guests > room.capacity()) {
            throw new CapacityExceededException();
        }

        if (reservationRepository.hasConflictingReservation(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = new Reservation(
            0,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            room.nightlyRate()
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();

        reservation.confirm();
        reservationRepository.update(reservation);

        for (ReservationEventListener listener : listeners) {
            listener.onReservationConfirmed(reservation, room.type());
        }

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();
        boolean wasPreviouslyConfirmed = reservation.wasPreviouslyConfirmed();

        reservation.cancel();
        reservationRepository.update(reservation);

        for (ReservationEventListener listener : listeners) {
            listener.onReservationCancelled(reservation, room.type(), wasPreviouslyConfirmed);
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        reservation.performCheckIn();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        reservation.performCheckOut();
        reservationRepository.update(reservation);

        return reservation;
    }

    public Optional<Reservation> getReservation(long id) {
        return reservationRepository.findById(id);
    }

    public List<Reservation> getOccupancyForDate(LocalDate date) {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream()
            .flatMap(room -> reservationRepository.findByRoomAndDateAndStatuses(
                room.number(),
                date,
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT)
            ).stream())
            .sorted((r1, r2) -> Integer.compare(r1.roomNumber(), r2.roomNumber()))
            .toList();
    }

    public List<Reservation> getRevenueForMonth(YearMonth month) {
        return reservationRepository.findByCheckInMonthAndStatuses(
            month,
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT, ReservationStatus.CANCELLED)
        );
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
    public static class CapacityExceededException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
}
