package br.com.mare.pousada;

import java.time.LocalDate;

public record BookingRequest(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
