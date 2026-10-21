package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.tariff.TarifPlanFactory;
import com.pousadamarealta.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
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
        // Validate room
        Room room = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Validate guests
        if (guests < 1 || guests > room.capacity) {
            throw new IllegalArgumentException("Invalid number of guests");
        }

        // Validate dates
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-in must be before check-out");
        }

        // Check availability
        checkRoomAvailability(roomNumber, checkIn, checkOut);

        // Calculate tariff
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        var tarifPlan = TarifPlanFactory.create(ratePlan);
        BigDecimal subtotal = tarifPlan.calculateSubtotal(room.nightlyRate, nights, checkIn);
        BigDecimal discount = tarifPlan.calculateDiscount(subtotal);
        BigDecimal total = Money.subtract(subtotal, discount);

        Reservation reservation = new Reservation(
            0,
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

        return reservationRepo.create(reservation);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot confirm non-pending reservation");
        }

        reservation.status = ReservationStatus.CONFIRMED;
        reservation.amountPaid = amountPaid;

        return reservationRepo.save(reservation);
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only check in confirmed reservations");
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        return reservationRepo.save(reservation);
    }

    public Reservation checkOut(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Can only check out checked-in reservations");
        }

        reservation.status = ReservationStatus.CHECKED_OUT;
        return reservationRepo.save(reservation);
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.status != ReservationStatus.PENDING && reservation.status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only cancel pending or confirmed reservations");
        }

        var tarifPlan = TarifPlanFactory.create(reservation.ratePlan);
        BigDecimal refund = tarifPlan.calculateRefund(reservation.amountPaid);

        reservation.status = ReservationStatus.CANCELLED;
        reservation.refund = refund;

        return reservationRepo.save(reservation);
    }

    private void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            var existing = reservationRepo.findByRoomAndNight(roomNumber, date);
            if (existing.iterator().hasNext()) {
                throw new IllegalArgumentException("Room not available");
            }
        }
    }
}
