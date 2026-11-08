package com.pousada.domain.state;

import com.pousada.domain.OperationNotAllowedException;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

public class CheckedOutState implements ReservationState {
    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CHECKED_OUT;
    }

    @Override
    public void confirm(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot confirm a checked-out reservation");
    }

    @Override
    public void cancel(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot cancel a checked-out reservation");
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new OperationNotAllowedException("Cannot check in a checked-out reservation");
    }

    @Override
    public void checkOut(Reservation reservation) {
        throw new OperationNotAllowedException("Reservation is already checked out");
    }
}
