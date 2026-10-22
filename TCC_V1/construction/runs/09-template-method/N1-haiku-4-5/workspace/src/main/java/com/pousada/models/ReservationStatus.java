package com.pousada.models;

public enum ReservationStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CHECKED_IN("CHECKED_IN"),
    CHECKED_OUT("CHECKED_OUT"),
    CANCELLED("CANCELLED");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ReservationStatus fromLabel(String label) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown reservation status: " + label);
    }
}
