package com.pousadamarealta.exception;

public class RoomNotFoundException extends ApiException {
    public RoomNotFoundException(String roomNumber) {
        super("ROOM_NOT_FOUND", "Room " + roomNumber + " not found", 404);
    }
}
