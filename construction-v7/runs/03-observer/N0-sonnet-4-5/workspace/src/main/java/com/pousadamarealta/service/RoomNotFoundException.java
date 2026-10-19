package com.pousadamarealta.service;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomNumber) {
        super("Room " + roomNumber + " not found");
    }
}
