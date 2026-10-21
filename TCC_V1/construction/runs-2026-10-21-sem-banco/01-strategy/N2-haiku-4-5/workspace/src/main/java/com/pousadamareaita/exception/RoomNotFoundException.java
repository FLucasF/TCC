package com.pousadamareaita.exception;

public class RoomNotFoundException extends ReservationException {
    public RoomNotFoundException(int roomNumber) {
        super("ROOM_NOT_FOUND", "Room " + roomNumber + " not found");
    }

    @Override
    public int getStatusCode() {
        return 404;
    }
}
