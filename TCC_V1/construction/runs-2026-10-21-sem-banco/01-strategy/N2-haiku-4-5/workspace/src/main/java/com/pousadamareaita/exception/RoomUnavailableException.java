package com.pousadamareaita.exception;

public class RoomUnavailableException extends ReservationException {
    public RoomUnavailableException(int roomNumber) {
        super("ROOM_UNAVAILABLE", "Room " + roomNumber + " is unavailable for the selected dates");
    }

    @Override
    public int getStatusCode() {
        return 409;
    }
}
