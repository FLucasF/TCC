package com.pousada.marealta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.util.PriceCalculator;

public class ReservationService {
    private final List<Reservation> reservations = new ArrayList<>();
    private final RoomService roomService;
    private final NotificationService notificationService;
    private long nextReservationId = 1;

    public ReservationService(RoomService roomService, NotificationService notificationService) {
        this.roomService = roomService;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Optional<Room> room = roomService.getRoomByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        Room r = room.get();

        if (guests < 1 || guests > r.getCapacity()) {
            throw new IllegalArgumentException(
                guests > r.getCapacity() ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST"
            );
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(r.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            nextReservationId++, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total
        );

        reservations.add(reservation);
        return reservation;
    }

    public Optional<Reservation> getReservation(long id) {
        return reservations.stream()
            .filter(r -> r.id == id)
            .findFirst();
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = amountPaid;

        notificationService.notifyConfirmation(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status == ReservationStatus.CHECKED_IN ||
            reservation.status == ReservationStatus.CHECKED_OUT ||
            reservation.status == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = reservation.status == ReservationStatus.CONFIRMED;
        reservation.status = ReservationStatus.CANCELLED;

        if (wasConfirmed) {
            if (reservation.ratePlan == RatePlan.STANDARD) {
                reservation.refund = reservation.amountPaid;
            } else {
                reservation.refund = BigDecimal.ZERO;
            }
            notificationService.notifyCancellation(reservation);
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        return reservation;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            LocalDate finalCurrent = current;
            boolean unavailable = reservations.stream()
                .filter(r -> r.roomNumber == roomNumber && r.status != ReservationStatus.CANCELLED)
                .anyMatch(r -> {
                    LocalDate resStart = r.checkIn;
                    LocalDate resEnd = r.checkOut;
                    return !finalCurrent.isBefore(resStart) && finalCurrent.isBefore(resEnd);
                });

            if (unavailable) return true;
            current = current.plusDays(1);
        }

        return false;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }
}
