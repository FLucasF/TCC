package com.pousada.marealta.service;

import com.pousada.marealta.domain.ReservationStatus;

public class ReservationStateMachine {
    private static final PendingState PENDING_STATE = new PendingState();
    private static final ConfirmedState CONFIRMED_STATE = new ConfirmedState();
    private static final CheckedInState CHECKED_IN_STATE = new CheckedInState();
    private static final CheckedOutState CHECKED_OUT_STATE = new CheckedOutState();
    private static final CancelledState CANCELLED_STATE = new CancelledState();

    public static ReservationState getState(ReservationStatus status) {
        return switch (status) {
            case PENDING -> PENDING_STATE;
            case CONFIRMED -> CONFIRMED_STATE;
            case CHECKED_IN -> CHECKED_IN_STATE;
            case CHECKED_OUT -> CHECKED_OUT_STATE;
            case CANCELLED -> CANCELLED_STATE;
        };
    }
}
