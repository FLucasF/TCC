package com.marealta.domain;

public final class HotelException extends RuntimeException {
    private final ErrorCode code;

    public HotelException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
