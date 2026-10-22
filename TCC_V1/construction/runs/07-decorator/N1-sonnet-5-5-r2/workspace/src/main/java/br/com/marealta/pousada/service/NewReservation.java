package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.RatePlan;
import java.time.LocalDate;

public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {
}
