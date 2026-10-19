package com.pousadamareaalta.exception;

public class RoomNotFound extends ApiException {
    public RoomNotFound() {
        super("ROOM_NOT_FOUND", "Room not found", 404);
    }
}
