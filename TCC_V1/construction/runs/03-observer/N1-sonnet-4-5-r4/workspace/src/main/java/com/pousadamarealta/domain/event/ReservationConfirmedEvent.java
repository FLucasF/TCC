package com.pousadamarealta.domain.event;

import com.pousadamarealta.domain.model.Reservation;

public class ReservationConfirmedEvent {
    private final Reservation reservation;
    private final String receiptText;

    public ReservationConfirmedEvent(Reservation reservation, String receiptText) {
        this.reservation = reservation;
        this.receiptText = receiptText;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public String getReceiptText() {
        return receiptText;
    }
}
