package com.pousadamarealta.service;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException() {
        super("Room is unavailable for the selected dates");
    }
}
