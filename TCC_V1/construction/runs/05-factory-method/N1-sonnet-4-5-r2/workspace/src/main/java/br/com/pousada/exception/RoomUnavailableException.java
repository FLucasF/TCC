package br.com.pousada.exception;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String roomNumber) {
        super("Room " + roomNumber + " is not available for the selected dates");
    }
}
