package br.com.marealta.pousada.app;

import br.com.marealta.pousada.domain.RatePlan;

import java.time.LocalDate;

public record BookingRequest(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
