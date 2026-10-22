package pousada.service;

import java.time.LocalDate;

import pousada.domain.RatePlan;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
