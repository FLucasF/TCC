package com.pousada.domain.state;

import com.pousada.domain.OperationNotAllowedException;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

public class CancelledState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CANCELLED;
    }

    @Override
    public void confirm(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot confirm a cancelled reservation");
    }

    @Override
    public void cancel(Reservation reservation) {
        throw new OperationNotAllowedException("Reservation is already cancelled");
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check in a cancelled reservation");
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check out a cancelled reservation");
    }
}
