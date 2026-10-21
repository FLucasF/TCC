package com.pousada.event;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;

public class ReservationCancelledEvent implements ReservationEvent {
    private final Reservation reservation;
    private final Money refundAmount;
    private final boolean wasConfirmed;

    public ReservationCancelledEvent(Reservation reservation, Money refundAmount, boolean wasConfirmed) {
        this.reservation = reservation;
        this.refundAmount = refundAmount;
        this.wasConfirmed = wasConfirmed;
    }

    @Override
    public Reservation getReservation() {
        return reservation;
    }

    public Money getRefundAmount() {
        return refundAmount;
    }

    public boolean wasConfirmed() {
        return wasConfirmed;
    }
}
