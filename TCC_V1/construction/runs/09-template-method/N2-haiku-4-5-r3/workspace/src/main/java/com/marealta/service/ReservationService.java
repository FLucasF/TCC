package com.marealta.service;

import com.marealta.db.ReservationRepository;
import com.marealta.db.RoomRepository;
import com.marealta.model.Reservation;
import com.marealta.model.Room;
import com.marealta.util.Money;
import com.marealta.util.Season;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                       LocalDate checkIn, LocalDate checkOut, int guests, Reservation.RatePlan ratePlan) {
        Optional<Room> room = roomRepository.findByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.get().capacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = calculateNightsCost(room.get(), checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = Money.subtract(subtotal, discount);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation confirm(long id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CHECKED_IN);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(Reservation.Status.CHECKED_OUT);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation cancel(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.PENDING && reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = reservation.getStatus() == Reservation.Status.CONFIRMED;
        reservation.setStatus(Reservation.Status.CANCELLED);

        if (wasConfirmed) {
            if (reservation.getRatePlan() == Reservation.RatePlan.STANDARD) {
                reservation.setRefund(reservation.getAmountPaid());
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    private BigDecimal calculateNightsCost(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal totalCost = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightCost = room.nightlyRate();

            if (Season.isHighSeason(current)) {
                BigDecimal markupFactor = BigDecimal.valueOf(100 + Season.getMarkupPercent()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_EVEN);
                nightCost = Money.multiply(nightCost, markupFactor);
            }

            nightCost = Money.round(nightCost);
            totalCost = Money.add(totalCost, nightCost);
            current = current.plusDays(1);
        }

        return totalCost;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, Reservation.RatePlan ratePlan) {
        if (ratePlan == Reservation.RatePlan.NON_REFUNDABLE) {
            return Money.percent(subtotal, 15);
        }
        return BigDecimal.ZERO;
    }
}
