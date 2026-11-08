package br.com.pousadamarealta.events;

import br.com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;

public class ReservationConfirmedEvent implements ReservationEvent {
    private final Reservation reservation;
    private final BigDecimal amountPaid;

    public ReservationConfirmedEvent(Reservation reservation, BigDecimal amountPaid) {
        this.reservation = reservation;
        this.amountPaid = amountPaid;
    }

    @Override
    public Reservation getReservation() {
        return reservation;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }
}
