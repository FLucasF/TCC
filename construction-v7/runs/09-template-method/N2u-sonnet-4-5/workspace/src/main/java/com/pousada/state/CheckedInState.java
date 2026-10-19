package com.pousada.state;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.math.BigDecimal;

public class CheckedInState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CHECKED_IN;
    }

    @Override
    public void confirm(Reservation reservation, BigDecimal amountPaid) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }

    @Override
    public void cancel(Reservation reservation) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }

    @Override
    public void checkOut(Reservation reservation) {
        reservation.setState(new CheckedOutState());
    }
}
