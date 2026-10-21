package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.notification.EventDispatcher;
import com.pousadamarealta.notification.event.ReservationCancelledEvent;
import com.pousadamarealta.notification.event.ReservationConfirmedEvent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);
    private final RoomService roomService;
    private final PricingService pricingService;
    private final EventDispatcher eventDispatcher;

    public ReservationService(RoomService roomService, PricingService pricingService, EventDispatcher eventDispatcher) {
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.eventDispatcher = eventDispatcher;
    }

    public Reservation createReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan
    ) throws ReservationException {
        Room room = roomService.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new ReservationException("ROOM_NOT_FOUND", "Quarto não encontrado");
        }

        if (guests > room.capacity()) {
            throw new ReservationException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto");
        }

        if (guests < 1) {
            throw new ReservationException("INVALID_REQUEST", "Pelo menos um hóspede é necessário");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ReservationException("INVALID_REQUEST", "Data de saída deve ser após a data de entrada");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new ReservationException("ROOM_UNAVAILABLE", "Quarto não está disponível para essas datas");
        }

        long nights = calculateNights(checkIn, checkOut);
        double subtotal = calculateSubtotal(room.nightlyRate(), checkIn, nights);
        double discount = pricingService.calculateDiscount(subtotal, ratePlan);
        double total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            idCounter.incrementAndGet(),
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            RatePlan.valueOf(ratePlan),
            (int) nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            0.0,
            null
        );

        reservations.put(reservation.id, reservation);
        return reservation;
    }

    public Reservation getReservation(long id) throws ReservationException {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
        return res;
    }

    public Reservation confirmReservation(long id) throws ReservationException {
        Reservation res = getReservation(id);

        if (res.status != ReservationStatus.PENDING) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Apenas reservas pendentes podem ser confirmadas");
        }

        Reservation updated = res
            .withStatus(ReservationStatus.CONFIRMED)
            .withAmountPaid(res.total);

        reservations.put(id, updated);
        eventDispatcher.dispatch(new ReservationConfirmedEvent(updated));
        return updated;
    }

    public Reservation checkIn(long id) throws ReservationException {
        Reservation res = getReservation(id);

        if (res.status != ReservationStatus.CONFIRMED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Apenas reservas confirmadas podem fazer check-in");
        }

        Reservation updated = res.withStatus(ReservationStatus.CHECKED_IN);
        reservations.put(id, updated);
        return updated;
    }

    public Reservation checkOut(long id) throws ReservationException {
        Reservation res = getReservation(id);

        if (res.status != ReservationStatus.CHECKED_IN) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Apenas reservas com check-in podem fazer check-out");
        }

        Reservation updated = res.withStatus(ReservationStatus.CHECKED_OUT);
        reservations.put(id, updated);
        return updated;
    }

    public Reservation cancelReservation(long id) throws ReservationException {
        Reservation res = getReservation(id);

        if (res.status == ReservationStatus.CHECKED_OUT || res.status == ReservationStatus.CANCELLED) {
            throw new ReservationException("OPERATION_NOT_ALLOWED", "Essa reserva não pode ser cancelada");
        }

        ReservationStatus previousStatus = res.status;
        Double refund = null;

        if (res.status == ReservationStatus.CONFIRMED || res.status == ReservationStatus.CHECKED_IN) {
            if (res.ratePlan == RatePlan.STANDARD) {
                refund = res.amountPaid;
            } else {
                refund = 0.0;
            }
        }

        Reservation updated = res
            .withStatus(ReservationStatus.CANCELLED)
            .withRefund(refund);

        reservations.put(id, updated);
        eventDispatcher.dispatch(new ReservationCancelledEvent(updated, previousStatus));
        return updated;
    }

    public List<Reservation> getAllReservations() {
        return List.copyOf(reservations.values());
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.roomNumber == roomNumber && r.status != ReservationStatus.CANCELLED)
            .anyMatch(r -> hasConflict(r, checkIn, checkOut));
    }

    private boolean hasConflict(Reservation existing, LocalDate checkIn, LocalDate checkOut) {
        return !checkOut.isBefore(existing.checkIn) && !checkIn.isAfter(existing.checkOut.minusDays(1));
    }

    private long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    private double calculateSubtotal(double baseRate, LocalDate checkIn, long nights) {
        double total = 0;
        LocalDate current = checkIn;
        for (int i = 0; i < nights; i++) {
            double nightlyRate = pricingService.calculateNightlyRate(baseRate, current);
            total += nightlyRate;
            current = current.plusDays(1);
        }
        return pricingService.calculateSubtotal(total, 1);
    }

    public static class ReservationException extends Exception {
        public final String errorCode;

        public ReservationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}
