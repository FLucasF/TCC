package br.com.pousada.marealta.api.dto;

import java.time.LocalDate;

public record CreateReservationRequest(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan
) {}
