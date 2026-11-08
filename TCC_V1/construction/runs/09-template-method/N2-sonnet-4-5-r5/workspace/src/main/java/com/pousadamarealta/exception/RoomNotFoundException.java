package com.pousadamarealta.exception;

public class RoomNotFoundException extends ApiException {
    public RoomNotFoundException(String message) {
        super("ROOM_NOT_FOUND", message, 404);
    }
}
