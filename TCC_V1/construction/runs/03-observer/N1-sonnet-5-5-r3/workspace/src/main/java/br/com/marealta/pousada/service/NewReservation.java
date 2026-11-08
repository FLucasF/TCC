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

    public NewReservation {
        if (!checkOut.isAfter(checkIn)) {
            throw ServiceException.invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ServiceException.invalid("É preciso pelo menos um hóspede");
        }
    }
}
