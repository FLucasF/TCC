package br.com.pousadamareal.domain;

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
}
