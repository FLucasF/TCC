package br.com.pousadamarealta.service;

public final class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String roomNumber) {
        super("Room unavailable: " + roomNumber);
    }
}
