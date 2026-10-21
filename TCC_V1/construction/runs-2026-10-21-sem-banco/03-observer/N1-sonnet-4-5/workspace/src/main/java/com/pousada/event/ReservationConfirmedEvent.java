package com.pousada.event;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;

public class ReservationConfirmedEvent implements ReservationEvent {
    private final Reservation reservation;
    private final Money total;

    public ReservationConfirmedEvent(Reservation reservation, Money total) {
        this.reservation = reservation;
        this.total = total;
    }

    @Override
    public Reservation getReservation() {
        return reservation;
    }

    public Money getTotal() {
        return total;
    }
}
