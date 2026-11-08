package com.pousada.domain.state;

import com.pousada.domain.OperationNotAllowedException;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

public class PendingState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.PENDING;
    }

    @Override
    public void confirm(Reservation reservation) {
        reservation.setAmountPaid(reservation.getTotal());
        reservation.setState(new ConfirmedState());
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.setRefund(reservation.getRatePlan().calculateRefund(reservation.getAmountPaid()));
        reservation.setState(new CancelledState());
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check in a pending reservation");
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check out a pending reservation");
    }
}
