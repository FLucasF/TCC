package com.pousadamarealta.service.exceptions;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String message) {
        super(message);
    }
}
