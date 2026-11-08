package com.pousada.marealta.domain.notification;

import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.shared.Money;

public class ReservationCancelled extends DomainEvent {
    private final Reservation reservation;
    private final Money refundAmount;

    public ReservationCancelled(Reservation reservation, Money refundAmount) {
        this.reservation = reservation;
        this.refundAmount = refundAmount;
    }

    public Reservation reservation() {
        return reservation;
    }

    public Money refundAmount() {
        return refundAmount;
    }
}
