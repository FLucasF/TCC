package pousada.domain;

import pousada.identity.GuestIdentity;

import java.time.LocalDate;

/** O que o site informa para criar uma reserva. */
public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        GuestIdentity identity,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {
}
