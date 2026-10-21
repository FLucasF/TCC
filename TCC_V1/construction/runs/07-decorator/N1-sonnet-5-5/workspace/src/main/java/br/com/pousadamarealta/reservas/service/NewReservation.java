package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.RatePlan;
import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
