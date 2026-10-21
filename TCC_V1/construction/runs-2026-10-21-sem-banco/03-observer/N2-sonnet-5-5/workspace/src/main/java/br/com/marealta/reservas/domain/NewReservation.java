package br.com.marealta.reservas.domain;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
