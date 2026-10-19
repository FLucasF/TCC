package com.pousada.state;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.math.BigDecimal;

public class ConfirmedState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public void confirm(Reservation reservation, BigDecimal amountPaid) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }

    @Override
    public void cancel(Reservation reservation) {
        BigDecimal refund = reservation.getRatePlanStrategy().isRefundable()
            ? reservation.getAmountPaid()
            : BigDecimal.ZERO;
        reservation.setRefund(refund);
        reservation.setState(new CancelledState());
    }

    @Override
    public void checkIn(Reservation reservation) {
        reservation.setState(new CheckedInState());
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new IllegalStateException("OPERATION_NOT_ALLOWED");
    }
}
