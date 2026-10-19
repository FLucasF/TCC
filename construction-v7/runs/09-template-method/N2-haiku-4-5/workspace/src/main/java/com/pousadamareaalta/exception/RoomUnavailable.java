package com.pousadamareaalta.exception;

public class RoomUnavailable extends ApiException {
    public RoomUnavailable() {
        super("ROOM_UNAVAILABLE", "Room is not available for the selected dates", 409);
    }
}
