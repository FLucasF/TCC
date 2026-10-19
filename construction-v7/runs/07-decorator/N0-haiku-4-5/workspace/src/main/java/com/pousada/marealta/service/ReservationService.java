package com.pousada.marealta.service;

import com.pousada.marealta.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final RoomManager roomManager;
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ReservationService(RoomManager roomManager) {
        this.roomManager = roomManager;
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
    ) throws ValidationException {
        var room = roomManager.getRoom(roomNumber)
            .orElseThrow(() -> new ValidationException("ROOM_NOT_FOUND", "Quarto não encontrado"));

        if (checkIn.isAfter(checkOut)) {
            throw new ValidationException("INVALID_REQUEST", "Data de saída não pode ser antes da entrada");
        }

        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "Pelo menos 1 hóspede é obrigatório");
        }

        if (guests > room.capacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, -1)) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Quarto não disponível para essas datas");
        }

        var pricing = PricingService.calculate(room, checkIn, checkOut, ratePlan);

        long id = nextId.getAndIncrement();
        Reservation reservation = new Reservation(
            id,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            pricing.nights,
            pricing.subtotal,
            pricing.discount,
            pricing.total
        );

        reservations.put(id, reservation);
        return reservation;
    }

    public Optional<Reservation> getReservation(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public void confirmReservation(long id, BigDecimal amountPaid) throws ValidationException {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Só é possível confirmar uma reserva pendente");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
    }

    public void checkin(long id) throws ValidationException {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Só é possível fazer check-in de uma reserva confirmada");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
    }

    public void checkout(long id) throws ValidationException {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Só é possível fazer check-out de uma reserva com check-in realizado");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
    }

    public void cancelReservation(long id) throws ValidationException {
        Reservation reservation = getReservation(id)
            .orElseThrow(() -> new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada"));

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Só é possível cancelar uma reserva pendente ou confirmada");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (status == ReservationStatus.CONFIRMED) {
            reservation.setRefund(refund);
        }
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, long excludeReservationId) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            for (Reservation res : reservations.values()) {
                if (res.getId() == excludeReservationId) {
                    continue;
                }

                if (res.getStatus() == ReservationStatus.CANCELLED) {
                    continue;
                }

                if (res.getRoomNumber() == roomNumber &&
                    !current.isBefore(res.getCheckIn()) &&
                    current.isBefore(res.getCheckOut())) {
                    return true;
                }
            }
            current = current.plusDays(1);
        }
        return false;
    }

    public static class ValidationException extends Exception {
        public final String code;

        public ValidationException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
