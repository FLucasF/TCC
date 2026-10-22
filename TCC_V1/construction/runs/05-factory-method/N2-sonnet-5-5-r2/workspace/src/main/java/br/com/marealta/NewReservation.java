package br.com.marealta;

import br.com.marealta.domain.Identity;
import br.com.marealta.domain.RatePlan;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, Identity identity,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
