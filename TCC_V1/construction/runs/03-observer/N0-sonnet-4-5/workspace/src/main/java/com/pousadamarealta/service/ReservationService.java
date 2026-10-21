package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final RoomService roomService;
    private final PricingService pricingService;
    private final EventService eventService;

    public ReservationService(RoomService roomService, PricingService pricingService, EventService eventService) {
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.eventService = eventService;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

        Room room = roomService.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            nextId.getAndIncrement(),
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

    public Optional<Reservation> findById(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation confirm(long id) {
        Reservation reservation = findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.confirm();
        eventService.publish(new ReservationConfirmedEvent(reservation));
        return reservation;
    }

    public Reservation cancel(long id) {
        Reservation reservation = findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING
            && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        ReservationStatus previousStatus = reservation.getStatus();
        reservation.cancel();
        eventService.publish(new ReservationCancelledEvent(reservation, previousStatus));
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.checkOut();
        return reservation;
    }

    public List<Reservation> getOccupancyForDate(LocalDate date) {
        return reservations.values().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(Reservation::isPaid)
            .filter(r -> r.occupiesDate(date))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .toList();
    }

    public List<Reservation> getRevenueForMonth(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> {
                BigDecimal revenue = r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO);
                return revenue.compareTo(BigDecimal.ZERO) > 0;
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .toList();
    }

    private boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .noneMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !start1.isAfter(end2.minusDays(1))
            && !end2.isBefore(start1) && !start2.isAfter(end1.minusDays(1));
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
    public static class OperationNotAllowedException extends RuntimeException {}
}
