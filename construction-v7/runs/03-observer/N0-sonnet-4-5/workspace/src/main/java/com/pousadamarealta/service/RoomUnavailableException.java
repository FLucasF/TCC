package com.pousadamarealta.service;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException() {
        super("Room is not available for the selected dates");
    }
}
