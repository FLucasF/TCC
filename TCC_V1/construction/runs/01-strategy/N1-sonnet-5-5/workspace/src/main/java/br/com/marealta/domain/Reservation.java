package br.com.marealta.domain;

import br.com.marealta.plan.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static br.com.marealta.domain.ReservationStatus.CANCELLED;
import static br.com.marealta.domain.ReservationStatus.CHECKED_IN;
import static br.com.marealta.domain.ReservationStatus.CHECKED_OUT;
import static br.com.marealta.domain.ReservationStatus.CONFIRMED;
import static br.com.marealta.domain.ReservationStatus.PENDING;

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
        BigDecimal refund) {

    private static final Set<ReservationStatus> PAID = EnumSet.of(CONFIRMED, CHECKED_IN, CHECKED_OUT);

    public static Reservation pending(Room room, String guestName, String guestEmail, String guestCpf,
                                      Stay stay, int guests, RatePlan plan, Pricing pricing) {
        return new Reservation(0, room.number(), guestName, guestEmail, guestCpf, stay.checkIn(), stay.checkOut(),
                guests, plan.code(), stay.nights(), pricing.subtotal(), pricing.discount(), pricing.total(),
                PENDING, Money.ZERO, null);
    }

    public Reservation withId(long newId) {
        return new Reservation(newId, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public boolean isPaid() {
        return PAID.contains(status);
    }

    public Reservation confirm() {
        requireStatus(PENDING, "Só é possível confirmar uma reserva pendente");
        return with(CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus(CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus(CHECKED_IN, "Só é possível fazer check-out de uma reserva com check-in");
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel(RatePlan plan) {
        if (status != PENDING && status != CONFIRMED) {
            throw ApiException.operationNotAllowed("Só é possível cancelar uma reserva pendente ou confirmada");
        }
        return with(CANCELLED, amountPaid, plan.refund(amountPaid));
    }

    public BigDecimal netRevenue() {
        return amountPaid.subtract(refund == null ? Money.ZERO : refund);
    }

    private void requireStatus(ReservationStatus expected, String message) {
        if (status != expected) {
            throw ApiException.operationNotAllowed(message);
        }
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
