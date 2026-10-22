package com.pousada.marealta.domain;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    public static ReservationStatus fromString(String name) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        return null;
    }
}
