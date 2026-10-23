package br.com.marealta.exception;

public class RoomNotFoundException extends PousadaException {
    public RoomNotFoundException(String roomNumber) {
        super("ROOM_NOT_FOUND", "Room " + roomNumber + " not found");
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }
}
