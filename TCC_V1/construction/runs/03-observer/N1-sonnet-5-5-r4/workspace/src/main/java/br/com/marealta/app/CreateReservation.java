package br.com.marealta.app;

import br.com.marealta.domain.RatePlan;

import java.time.LocalDate;

public record CreateReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {
}
