package com.pousada.domain.state;

import com.pousada.domain.OperationNotAllowedException;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

public class ConfirmedState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public void confirm(Reservation reservation) {
        throw new OperationNotAllowedException("Reservation is already confirmed");
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.setRefund(reservation.getRatePlan().calculateRefund(reservation.getAmountPaid()));
        reservation.setState(new CancelledState());
    }

    @Override
    public void checkIn(Reservation reservation) {
        reservation.setState(new CheckedInState());
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check out without checking in first");
    }
}
