package com.pousadamarealta.service.exceptions;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String message) {
        super(message);
    }
}
