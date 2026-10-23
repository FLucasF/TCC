package br.com.pousada.maralta.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    @JsonValue
    public String toJson() {
        return name();
    }
}
