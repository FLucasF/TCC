package com.pousada.domain;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    public boolean canConfirm() {
        return this == PENDING;
    }

    public boolean canCheckIn() {
        return this == CONFIRMED;
    }

    public boolean canCheckOut() {
        return this == CHECKED_IN;
    }

    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isActive() {
        return this != CANCELLED;
    }

    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
