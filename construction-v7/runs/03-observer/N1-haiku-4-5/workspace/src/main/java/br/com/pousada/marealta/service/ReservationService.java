package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.util.DateUtil;
import br.com.pousada.marealta.util.MoneyUtil;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReservationService {
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private long nextReservationId = 1;
    private final NotificationService notificationService;

    public ReservationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, Room.from(101, RoomType.STANDARD));
        rooms.put(102, Room.from(102, RoomType.STANDARD));
        rooms.put(201, Room.from(201, RoomType.DELUXE));
        rooms.put(202, Room.from(202, RoomType.DELUXE));
        rooms.put(301, Room.from(301, RoomType.SUITE));
    }

    public List<Room> getRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparingInt(Room::number))
                .toList();
    }

    public Room getRoom(int roomNumber) {
        return rooms.get(roomNumber);
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
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.capacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        double subtotal = 0;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            boolean isHighSeason = DateUtil.isHighSeason(date);
            double nightlyRate = MoneyUtil.calculateNightlyRate(room.nightlyRate(), isHighSeason);
            subtotal += nightlyRate;
        }
        subtotal = MoneyUtil.round(subtotal);

        double discount = MoneyUtil.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        double total = MoneyUtil.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
                nextReservationId++,
                roomNumber,
                guestName,
                guestEmail,
                guestCpf,
                checkIn,
                checkOut,
                guests,
                ratePlan,
                nights,
                subtotal,
                discount,
                total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    public Reservation confirm(long id, double amountPaid) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.confirm(amountPaid);
        notificationService.notifyReservationConfirmed(reservation, rooms.get(reservation.getRoomNumber()).type());
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.checkOut();
        return reservation;
    }

    public Reservation cancel(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Double refundAmount = null;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refundAmount = reservation.getAmountPaid();
            } else {
                refundAmount = 0.0;
            }
            notificationService.notifyReservationCancelled(reservation, rooms.get(reservation.getRoomNumber()).type(), refundAmount);
        }

        reservation.cancel(refundAmount);
        return reservation;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservations.values()) {
            if (res.getRoomNumber() != roomNumber) {
                continue;
            }

            if (res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
                if (!date.isBefore(res.getCheckIn()) && date.isBefore(res.getCheckOut())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }
}
