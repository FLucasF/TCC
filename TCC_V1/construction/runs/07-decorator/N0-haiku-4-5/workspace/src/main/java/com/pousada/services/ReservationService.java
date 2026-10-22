package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.DateUtil;
import com.pousada.utils.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, String ratePlan) throws Exception {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            return null;
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Invalid dates");
        }

        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room unavailable");
        }

        int nights = DateUtil.countNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room.getNightlyRate(), checkIn, checkOut, nights);
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
        reservation.setStatus("PENDING");
        reservation.setAmountPaid(new BigDecimal("0.00"));

        return reservationRepository.create(reservation);
    }

    public Reservation confirmReservation(Long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            return null;
        }

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalStateException("Cannot confirm a non-pending reservation");
        }

        reservation.setStatus("CONFIRMED");
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            return null;
        }

        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new IllegalStateException("Cannot check in a non-confirmed reservation");
        }

        reservation.setStatus("CHECKED_IN");
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            return null;
        }

        if (!"CHECKED_IN".equals(reservation.getStatus())) {
            throw new IllegalStateException("Cannot check out a non-checked-in reservation");
        }

        reservation.setStatus("CHECKED_OUT");
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            return null;
        }

        String status = reservation.getStatus();
        if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
            throw new IllegalStateException("Cannot cancel a reservation in this status");
        }

        BigDecimal refund = BigDecimal.ZERO;

        if ("CONFIRMED".equals(status)) {
            if ("STANDARD".equals(reservation.getRatePlan())) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.setStatus("CANCELLED");
        reservation.setRefund(refund);
        reservationRepository.update(reservation);

        return reservation;
    }

    private BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal total = BigDecimal.ZERO;

        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            BigDecimal dailyRate = nightlyRate;
            if (DateUtil.isHighSeason(currentDate)) {
                dailyRate = Money.multiply(nightlyRate, new BigDecimal("1.20"));
            }
            total = Money.add(total, dailyRate);
            currentDate = currentDate.plusDays(1);
        }

        return total;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return Money.percentage(subtotal, 15);
        }
        return BigDecimal.ZERO;
    }
}
