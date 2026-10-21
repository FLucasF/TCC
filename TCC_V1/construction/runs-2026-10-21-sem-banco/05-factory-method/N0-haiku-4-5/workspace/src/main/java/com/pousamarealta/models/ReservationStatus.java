package com.pousamarealta.models;

public enum ReservationStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CHECKED_IN("CHECKED_IN"),
    CHECKED_OUT("CHECKED_OUT"),
    CANCELLED("CANCELLED");

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReservationStatus fromString(String value) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + value);
    }
}
