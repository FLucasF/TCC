package br.com.pousada.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomNumber) {
        super("Room not found: " + roomNumber);
    }
}
