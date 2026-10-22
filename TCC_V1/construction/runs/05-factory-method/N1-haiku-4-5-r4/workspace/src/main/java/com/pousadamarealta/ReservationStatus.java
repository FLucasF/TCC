package com.pousadamarealta;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CHECKED_IN("CHECKED_IN"),
    CHECKED_OUT("CHECKED_OUT"),
    CANCELLED("CANCELLED");

    @JsonValue
    public final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    public static ReservationStatus fromString(String value) {
        return ReservationStatus.valueOf(value);
    }
}
