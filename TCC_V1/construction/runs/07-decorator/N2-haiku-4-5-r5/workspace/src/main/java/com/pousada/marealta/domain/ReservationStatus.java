package com.pousada.marealta.domain;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    public static ReservationStatus fromString(String value) {
        return ReservationStatus.valueOf(value);
    }
}
