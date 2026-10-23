package com.pousada.service;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String roomNumber) {
        super("Room " + roomNumber + " is not available for the requested dates");
    }
}
