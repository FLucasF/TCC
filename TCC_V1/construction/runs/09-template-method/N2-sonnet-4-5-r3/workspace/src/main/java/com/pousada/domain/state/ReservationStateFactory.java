package com.pousada.domain.state;

import com.pousada.domain.ReservationStatus;

public class ReservationStateFactory {
    public static ReservationState create(ReservationStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case CONFIRMED -> new ConfirmedState();
            case CHECKED_IN -> new CheckedInState();
            case CHECKED_OUT -> new CheckedOutState();
            case CANCELLED -> new CancelledState();
        };
    }
}
