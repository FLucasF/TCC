package br.com.marealta.api.dto;

import br.com.marealta.domain.RatePlan;

import java.time.LocalDate;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    RatePlan ratePlan
) {
}
