package com.pousada.state;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.math.BigDecimal;

public class PendingState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.PENDING;
    }

    @Override
    public void confirm(Reservation reservation, BigDecimal amountPaid) {
        reservation.setAmountPaid(amountPaid);
        reservation.setState(new ConfirmedState());
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.setRefund(BigDecimal.ZERO);
        reservation.setState(new CancelledState());
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }
}
