package br.com.marealta;

import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    @JsonProperty("nights")
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    @JsonIgnore
    public RatePlan plan() {
        return RatePlans.find(ratePlan).orElseThrow();
    }

    @JsonIgnore
    public Room room() {
        return Rooms.find(roomNumber).orElseThrow();
    }

    /** Pago menos devolvido: o que fica com a pousada. */
    @JsonIgnore
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public Reservation confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        return with(ReservationStatus.CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus("com check-in", ReservationStatus.CONFIRMED);
        return with(ReservationStatus.CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus("com check-out", ReservationStatus.CHECKED_IN);
        return with(ReservationStatus.CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        BigDecimal refunded = status == ReservationStatus.PENDING ? Money.ZERO : plan().refund(amountPaid);
        return with(ReservationStatus.CANCELLED, amountPaid, refunded);
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw ApiError.operationNotAllowed("Só existe comprovante de reserva paga");
        }
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (s == status) {
                return;
            }
        }
        throw ApiError.operationNotAllowed("Reserva " + status + " não pode ser " + action);
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
