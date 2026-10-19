package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.notification.ReservationCancelledEvent;
import com.pousadamarealta.notification.ReservationConfirmedEvent;

import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long reservationIdCounter = 1;

    public ReservationService(RoomService roomService, NotificationService notificationService) {
        this.roomService = roomService;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        Reservation reservation = new Reservation(
            reservationIdCounter++,
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

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .noneMatch(r -> overlaps(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public Optional<Reservation> getReservation(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.confirm();
            notificationService.notify(new ReservationConfirmedEvent(reservation));
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkInReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.checkIn();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation checkOutReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        try {
            reservation.checkOut();
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ReservationNotFoundException(id));

        ReservationStatus previousStatus = reservation.getStatus();

        try {
            reservation.cancel();
            notificationService.notify(new ReservationCancelledEvent(reservation, previousStatus));
            return reservation;
        } catch (IllegalStateException e) {
            throw new OperationNotAllowedException(e.getMessage());
        }
    }

    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations.values());
    }
}
