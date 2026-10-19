package com.pousadamarealta.service;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(long id) {
        super("Reservation " + id + " not found");
    }
}
