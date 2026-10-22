package br.com.marealta.domain;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    public NewReservation {
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("A saída precisa ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalidRequest("É preciso pelo menos um hóspede");
        }
    }
}
