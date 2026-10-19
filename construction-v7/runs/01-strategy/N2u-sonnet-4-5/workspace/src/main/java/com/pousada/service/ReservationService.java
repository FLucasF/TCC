package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.domain.rateplan.CorporateRatePlan;
import com.pousada.domain.rateplan.RatePlan;
import com.pousada.domain.rateplan.RatePlanFactory;
import com.pousada.util.DateUtils;
import com.pousada.util.ReceiptFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlanType ratePlanType, String companyCode) {
        Room room = roomService.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        int nights = (int) DateUtils.calculateNights(checkIn, checkOut);

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);

        ValidationResult validation = ratePlan.validate(checkIn, checkOut, nights, companyCode);
        if (!validation.isValid()) {
            throw new InvalidRequestException(validation.getErrorMessage());
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException("Room not available for selected dates");
        }

        PriceCalculation price;
        if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof CorporateRatePlan corporatePlan) {
            price = corporatePlan.calculatePriceWithCompany(room, checkIn, nights, companyCode);
        } else {
            price = ratePlan.calculatePrice(room, checkIn, checkOut, nights);
        }

        Long id = idGenerator.getAndIncrement();
        Reservation reservation = new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlanType, companyCode,
            nights, price.getSubtotal(), price.getDiscount(), price.getTotal()
        );

        reservations.put(id, reservation);
        return reservation;
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);
        reservation.confirm();

        String receipt = ReceiptFormatter.formatReceipt(reservation, getRoomType(reservation.getRoomNumber()));
        String emailBody = ReceiptFormatter.formatConfirmationEmail(reservation, getRoomType(reservation.getRoomNumber()));

        messageService.send(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        RatePlan ratePlan = RatePlanFactory.create(reservation.getRatePlanType());
        BigDecimal refund = ratePlan.calculateRefund(reservation.getAmountPaid());

        reservation.cancel(refund);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);
        reservation.checkOut();
        return reservation;
    }

    public Reservation getReservation(Long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ReservationNotFoundException("Reservation not found: " + id);
        }
        return reservation;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(Reservation::isActive)
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private RoomType getRoomType(String roomNumber) {
        return roomService.findByNumber(roomNumber)
            .map(Room::getType)
            .orElseThrow();
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

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
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

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }
}
