package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.*;
import com.pousadamarealta.domain.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PricingService pricingService;
    private final EventBus eventBus;

    public ReservationService(ReservationRepository reservationRepository,
                             PricingService pricingService,
                             EventBus eventBus) {
        this.reservationRepository = reservationRepository;
        this.pricingService = pricingService;
        this.eventBus = eventBus;
    }

    public Reservation createReservation(String roomNumber, Room room, String guestName,
                                        String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Long id = reservationRepository.getNextId();

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = calculateSubtotal(room.getNightlyRate(), checkIn, nights);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = subtotal.subtract(discount);

        Reservation reservation = new Reservation(
            id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights,
            subtotal, discount, total
        );

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in current status");
        }

        reservation.confirm();
        reservationRepository.save(reservation);

        eventBus.publish(new ReservationConfirmedEvent(reservation));

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in current status");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        reservation.cancel();
        reservationRepository.save(reservation);

        eventBus.publish(new ReservationCancelledEvent(reservation, wasConfirmed));

        return reservation;
    }

    public Reservation checkInReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canCheckIn()) {
            throw new IllegalStateException("Cannot check-in reservation in current status");
        }

        reservation.checkIn();
        return reservationRepository.save(reservation);
    }

    public Reservation checkOutReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.canCheckOut()) {
            throw new IllegalStateException("Cannot check-out reservation in current status");
        }

        reservation.checkOut();
        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> getReservation(Long id) {
        return reservationRepository.findById(id);
    }

    public boolean hasRoomConflict(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null);
    }

    private BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, night);
            subtotal = subtotal.add(nightPrice);
        }

        return subtotal.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
