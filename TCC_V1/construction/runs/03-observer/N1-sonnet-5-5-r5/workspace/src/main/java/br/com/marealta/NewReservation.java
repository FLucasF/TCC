package br.com.marealta;

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
            throw DomainException.invalidRequest("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw DomainException.invalidRequest("É preciso ao menos um hóspede");
        }
    }
}
