package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.domain.rateplans.RatePlan;
import com.pousada.domain.rateplans.RatePlanFactory;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService {
    private final Map<Integer, Room> rooms = new HashMap<>();
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextReservationId = 1;

    public ReservationService() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, RoomType.STANDARD));
        rooms.put(102, new Room(102, RoomType.STANDARD));
        rooms.put(201, new Room(201, RoomType.DELUXE));
        rooms.put(202, new Room(202, RoomType.DELUXE));
        rooms.put(301, new Room(301, RoomType.SUITE));
    }

    public Room getRoom(int roomNumber) {
        return rooms.get(roomNumber);
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, String ratePlanName, String companyCode) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanName, companyCode);

        try {
            ratePlan.validate(room, checkIn, checkOut, guests, companyCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = DateUtils.countNights(checkIn, checkOut);
        RatePlan.PriceCalculation prices = ratePlan.calculatePrices(room, checkIn, checkOut);

        Reservation reservation = new Reservation(
            nextReservationId++,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlanName,
            nights,
            prices.subtotal,
            prices.discount,
            prices.total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .noneMatch(r -> overlaps(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public void confirmReservation(long id, double amountPaid) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
    }

    public void checkIn(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
    }

    public void checkOut(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
    }

    public void cancelReservation(long id, RatePlan ratePlan) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        double refund = ratePlan.calculateRefund(reservation.getAmountPaid());
        reservation.setRefund(Money.round(refund));
    }

    public List<Reservation> getReservationsForRoom(int roomNumber, LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.getCheckIn().compareTo(date) <= 0 && r.getCheckOut().compareTo(date) > 0)
            .collect(Collectors.toList());
    }

    public List<Reservation> getReservationsStartingInMonth(java.time.YearMonth month) {
        return reservations.values().stream()
            .filter(r -> DateUtils.getYearMonth(r.getCheckIn()).equals(month))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED || r.getAmountPaid() > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
            .collect(Collectors.toList());
    }
}
