package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final RoomService roomService;

    public ReservationService(RoomService roomService) {
        this.roomService = roomService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room " + roomNumber + " not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.capacity()) {
            throw new CapacityExceededException("Room " + roomNumber + " has capacity for " +
                room.capacity() + " guests");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException("Room " + roomNumber + " is not available for the selected dates");
        }

        long id = nextId.getAndIncrement();
        Reservation reservation = new Reservation(id, roomNumber, room.type(), room.nightlyRate(),
            guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

        reservations.put(id, reservation);
        return reservation;
    }

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation getById(long id) {
        return findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));
    }

    public void confirm(long id) {
        Reservation reservation = getById(id);
        try {
            reservation.confirm();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public void checkIn(long id) {
        Reservation reservation = getById(id);
        try {
            reservation.doCheckIn();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public void checkOut(long id) {
        Reservation reservation = getById(id);
        try {
            reservation.doCheckOut();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public void cancel(long id) {
        Reservation reservation = getById(id);
        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public List<Reservation> getReservationsForNight(LocalDate night) {
        return reservations.values().stream()
            .filter(r -> r.status() != ReservationStatus.CANCELLED)
            .filter(r -> r.status() != ReservationStatus.PENDING)
            .filter(r -> r.occupiedNights().contains(night))
            .sorted(Comparator.comparing(Reservation::roomNumber))
            .toList();
    }

    public List<Reservation> getReservationsForMonth(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.checkIn().getYear() == year && r.checkIn().getMonthValue() == month)
            .filter(r -> {
                if (r.status() == ReservationStatus.CANCELLED) {
                    return r.amountPaid().subtract(
                        r.refund() != null ? r.refund() : r.amountPaid()
                    ).compareTo(java.math.BigDecimal.ZERO) > 0;
                }
                return r.status() != ReservationStatus.PENDING;
            })
            .sorted(Comparator.comparing(Reservation::checkIn)
                .thenComparing(Reservation::id))
            .toList();
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> requestedNights = checkIn.datesUntil(checkOut).toList();

        return reservations.values().stream()
            .filter(r -> r.roomNumber().equals(roomNumber))
            .filter(r -> r.status() != ReservationStatus.CANCELLED)
            .flatMap(r -> r.occupiedNights().stream())
            .anyMatch(requestedNights::contains);
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
