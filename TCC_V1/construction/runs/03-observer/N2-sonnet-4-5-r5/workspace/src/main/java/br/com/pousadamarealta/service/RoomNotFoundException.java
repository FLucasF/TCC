package br.com.pousadamarealta.service;

public final class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomNumber) {
        super("Room not found: " + roomNumber);
    }
}
