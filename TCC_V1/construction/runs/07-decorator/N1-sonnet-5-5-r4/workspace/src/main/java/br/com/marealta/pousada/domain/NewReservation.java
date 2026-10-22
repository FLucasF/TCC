package br.com.marealta.pousada.domain;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf, LocalDate checkIn,
                             LocalDate checkOut, int guests, RatePlan ratePlan) {

    public NewReservation {
        if (!checkOut.isAfter(checkIn)) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
    }
}
