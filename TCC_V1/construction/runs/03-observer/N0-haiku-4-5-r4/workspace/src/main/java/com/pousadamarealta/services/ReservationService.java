package com.pousadamarealta.services;

import com.pousadamarealta.exceptions.ApiException;
import com.pousadamarealta.models.*;
import com.pousadamarealta.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    public ReservationService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        PricingService pricingService,
        NotificationService notificationService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
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
    ) throws Exception {
        validateReservationInput(roomNumber, checkIn, checkOut, guests);

        Optional<Room> roomOpt = roomRepository.findByNumber(roomNumber);
        if (roomOpt.isEmpty()) {
            throw new ApiException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
        }

        Room room = roomOpt.get();
        if (guests > room.capacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto", 422);
        }

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        int nights = pricingService.countNights(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.nightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

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

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(int id) throws Exception {
        Optional<Reservation> resOpt = reservationRepository.findById(id);
        if (resOpt.isEmpty()) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }
        return resOpt.get();
    }

    public Reservation confirmReservation(int id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Apenas reservas pendentes podem ser confirmadas", 409);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);

        notificationService.notifyReservationConfirmed(reservation);

        return reservation;
    }

    public Reservation checkIn(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Apenas reservas confirmadas podem fazer check-in", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Apenas reservas com check-in feito podem fazer check-out", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(int id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                "Reserva não pode ser cancelada neste estado", 409);
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (wasConfirmed) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refundAmount = reservation.getAmountPaid();
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(refundAmount);
        }

        reservationRepository.update(reservation);

        if (wasConfirmed) {
            notificationService.notifyReservationCancelled(reservation);
        }

        return reservation;
    }

    private void validateReservationInput(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests)
            throws ApiException {
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Número de hóspedes inválido", 400);
        }

        if (checkIn.isAfter(checkOut)) {
            throw new ApiException("INVALID_REQUEST", "Data de saída deve ser após a de entrada", 400);
        }
    }

    private void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws ApiException {
        try {
            List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
            if (!conflicting.isEmpty()) {
                throw new ApiException("ROOM_UNAVAILABLE", "Quarto não está disponível nessas datas", 409);
            }
        } catch (Exception e) {
            if (e instanceof ApiException) throw (ApiException) e;
            throw new ApiException("INVALID_REQUEST", "Erro ao verificar disponibilidade", 400);
        }
    }
}
