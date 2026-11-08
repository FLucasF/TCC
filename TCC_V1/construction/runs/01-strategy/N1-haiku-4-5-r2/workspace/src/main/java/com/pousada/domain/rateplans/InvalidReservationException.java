package com.pousada.domain.rateplans;

public class InvalidReservationException extends RuntimeException {
    private final String code;

    public InvalidReservationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
