package com.pousadamarealta.exception;

public class RoomUnavailableException extends ApiException {
    public RoomUnavailableException(String message) {
        super("ROOM_UNAVAILABLE", message, 409);
    }
}
