package pousada.service;

import pousada.domain.RatePlan;
import pousada.identity.Identity;

import java.time.LocalDate;

public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        Identity identity,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {
}
