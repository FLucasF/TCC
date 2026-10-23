package br.com.marealta.exception;

public class RoomUnavailableException extends PousadaException {
    public RoomUnavailableException(String message) {
        super("ROOM_UNAVAILABLE", message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }
}
