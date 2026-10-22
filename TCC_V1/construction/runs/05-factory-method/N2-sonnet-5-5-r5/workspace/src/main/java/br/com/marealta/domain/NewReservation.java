package br.com.marealta.domain;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, GuestIdentity identity,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
