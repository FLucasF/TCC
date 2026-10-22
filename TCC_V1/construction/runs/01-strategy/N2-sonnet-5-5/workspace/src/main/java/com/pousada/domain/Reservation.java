package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pousada.error.ApiException;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund,
        @JsonIgnore String companyCode) {

    public Reservation confirm() {
        requireStatus("confirmar", ReservationStatus.PENDING);
        return with(ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation checkInGuest() {
        requireStatus("fazer check-in de", ReservationStatus.CONFIRMED);
        return with(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation checkOutGuest() {
        requireStatus("fazer check-out de", ReservationStatus.CHECKED_IN);
        return with(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel(BigDecimal refundAmount) {
        requireStatus("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        return with(ReservationStatus.CANCELLED, amountPaid, refundAmount);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw ApiException.operationNotAllowed("Não é possível " + action + " uma reserva " + status);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund, companyCode);
    }
}
