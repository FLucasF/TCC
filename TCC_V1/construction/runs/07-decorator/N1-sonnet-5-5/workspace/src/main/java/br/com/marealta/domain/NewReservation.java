package br.com.marealta.domain;

import java.time.LocalDate;

public record NewReservation(int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests,
                             RatePlan ratePlan) {
}
