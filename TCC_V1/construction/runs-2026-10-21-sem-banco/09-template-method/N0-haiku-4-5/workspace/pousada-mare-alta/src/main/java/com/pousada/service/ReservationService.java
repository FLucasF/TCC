package com.pousada.service;

import com.pousada.model.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.util.PricingUtils;
import com.pousada.util.SeasonUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RuntimeException("ROOM_NOT_FOUND"));

        if (guests < 1) {
            throw new RuntimeException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new RuntimeException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new RuntimeException("INVALID_REQUEST");
        }

        // Check room availability
        var existingReservations = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!existingReservations.isEmpty()) {
            throw new RuntimeException("ROOM_UNAVAILABLE");
        }

        // Calculate nights and prices
        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut, nights);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingUtils.round(subtotal.subtract(discount));

        Reservation reservation = new Reservation(0, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("RESERVATION_NOT_FOUND"));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Calculate refund
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            // Refund full amount if paid
            if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                reservation.setRefund(reservation.getAmountPaid());
            }
        } else {
            // NON_REFUNDABLE: no refund
            reservation.setRefund(BigDecimal.ZERO);
        }

        return reservationRepository.save(reservation);
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal total = BigDecimal.ZERO;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.getNightlyRate();

            if (SeasonUtils.isHighSeason(current)) {
                nightlyRate = PricingUtils.round(nightlyRate.multiply(BigDecimal.valueOf(1.20)));
            }

            total = total.add(nightlyRate);
            current = current.plusDays(1);
        }

        return PricingUtils.round(total);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return PricingUtils.percent(subtotal, 15);
        }
        return BigDecimal.ZERO;
    }
}
