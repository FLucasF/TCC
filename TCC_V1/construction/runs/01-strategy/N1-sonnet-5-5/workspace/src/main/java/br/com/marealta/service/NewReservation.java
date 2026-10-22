package br.com.marealta.service;

import java.time.LocalDate;

/** Dados já validados (formato) de um pedido de reserva. */
public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        String companyCode) {
}
