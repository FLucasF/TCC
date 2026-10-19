package com.pousadamarealta.domain;

public enum ReservationStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CHECKED_IN("CHECKED_IN"),
    CHECKED_OUT("CHECKED_OUT"),
    CANCELLED("CANCELLED");

    private final String code;

    ReservationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
