package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.util.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, Reservation.RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("Quarto não encontrado"));

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("Número de hóspedes inválido");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("Datas inválidas");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Quarto não disponível");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(room.getNightlyRate(), checkIn);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(nightlyRate, nights);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirm(int reservationId, BigDecimal amountPaid) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("Operação não permitida");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(int reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("Operação não permitida");
        }

        reservation.setStatus(Reservation.Status.CHECKED_IN);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(int reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new IllegalArgumentException("Operação não permitida");
        }

        reservation.setStatus(Reservation.Status.CHECKED_OUT);
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation cancel(int reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        if (reservation.getStatus() == Reservation.Status.CHECKED_IN ||
            reservation.getStatus() == Reservation.Status.CHECKED_OUT ||
            reservation.getStatus() == Reservation.Status.CANCELLED) {
            throw new IllegalArgumentException("Operação não permitida");
        }

        Reservation.Status previousStatus = reservation.getStatus();
        reservation.setStatus(Reservation.Status.CANCELLED);

        if (previousStatus == Reservation.Status.CONFIRMED) {
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

    public Optional<Reservation> findById(int reservationId) {
        return reservationRepository.findById(reservationId);
    }
}
