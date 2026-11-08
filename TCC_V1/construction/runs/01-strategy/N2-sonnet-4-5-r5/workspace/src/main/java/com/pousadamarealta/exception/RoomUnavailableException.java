package com.pousadamarealta.exception;

public class RoomUnavailableException extends ApiException {
    public RoomUnavailableException(String roomNumber) {
        super("ROOM_UNAVAILABLE", "Room " + roomNumber + " is not available for the selected dates", 409);
    }
}
