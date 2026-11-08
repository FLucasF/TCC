package com.pousadamarealta.service;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.rateplan.RatePlanFactory;
import com.pousadamarealta.service.rateplan.RatePlanStrategy;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RatePlanFactory ratePlanFactory;
    private final NotificationService notificationService;
    private final PricingService pricingService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             RatePlanFactory ratePlanFactory,
                             NotificationService notificationService,
                             PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.ratePlanFactory = ratePlanFactory;
        this.notificationService = notificationService;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Room not found: " + reservation.getRoomNumber()));

        validateReservation(reservation, room);

        RatePlanStrategy strategy = ratePlanFactory.getStrategy(reservation.getRatePlan());
        strategy.validate(reservation, room);

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = strategy.calculateSubtotal(reservation, room);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = strategy.calculateDiscount(subtotal, reservation);
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.round(subtotal.subtract(discount));
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    private void validateReservation(Reservation reservation, Room room) {
        if (reservation.getCheckOut().isBefore(reservation.getCheckIn()) ||
            reservation.getCheckOut().equals(reservation.getCheckIn())) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (reservation.getGuests() < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (reservation.getGuests() > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                String.format("Room capacity is %d, requested %d guests", room.getCapacity(), reservation.getGuests()));
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getId()
        );

        if (!conflicts.isEmpty()) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        }
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found: " + id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        notificationService.sendConfirmationEmail(reservation, room.getType().name());

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot cancel reservation in current status");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        RatePlanStrategy strategy = ratePlanFactory.getStrategy(reservation.getRatePlan());
        BigDecimal refundAmount = strategy.calculateRefund(reservation);
        reservation.setRefund(pricingService.round(refundAmount));

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public String generateReceipt(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Receipt is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return notificationService.formatReceipt(reservation, room.getType().name());
    }
}
