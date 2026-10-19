package com.pousadamareaalta.exception;

public class InvalidRequest extends ApiException {
    public InvalidRequest() {
        super("INVALID_REQUEST", "Invalid request", 400);
    }

    public InvalidRequest(String message) {
        super("INVALID_REQUEST", message, 400);
    }
}
