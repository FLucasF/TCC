package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextId = 1;
    private final RoomService roomService;

    public ReservationService(RoomService roomService) {
        this.roomService = roomService;
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
        Room room = roomService.getRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomBooked(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        PriceCalculator.PriceResult priceResult = PriceCalculator.calculate(
            room.getNightlyRate(),
            checkIn,
            checkOut,
            ratePlan.getCode()
        );

        Reservation reservation = new Reservation(
            nextId++,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            priceResult.subtotal,
            priceResult.discount,
            priceResult.total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }

    public void confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
    }

    public void checkIn(long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
    }

    public void checkOut(long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
    }

    public void cancelReservation(long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
    }

    private boolean isRoomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .anyMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate checkIn1, LocalDate checkOut1, LocalDate checkIn2, LocalDate checkOut2) {
        return checkIn1.isBefore(checkOut2) && checkOut1.isAfter(checkIn2);
    }

    public List<Reservation> getReservationsByRoom(int roomNumber, LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
            .collect(Collectors.toList());
    }

    public List<Reservation> getReservationsByMonth(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED || r.getAmountPaid().signum() > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
            .collect(Collectors.toList());
    }
}
