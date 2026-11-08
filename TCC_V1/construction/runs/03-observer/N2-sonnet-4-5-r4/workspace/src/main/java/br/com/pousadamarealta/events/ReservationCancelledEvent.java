package br.com.pousadamarealta.events;

import br.com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;

public class ReservationCancelledEvent implements ReservationEvent {
    private final Reservation reservation;
    private final boolean wasConfirmed;
    private final BigDecimal refundAmount;

    public ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed, BigDecimal refundAmount) {
        this.reservation = reservation;
        this.wasConfirmed = wasConfirmed;
        this.refundAmount = refundAmount;
    }

    @Override
    public Reservation getReservation() {
        return reservation;
    }

    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
}
