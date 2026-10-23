package br.com.pousada.maralta.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RatePlan {
    STANDARD,
    NON_REFUNDABLE;

    @JsonValue
    public String toJson() {
        return name();
    }
}
