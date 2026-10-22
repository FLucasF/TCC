package pousada.app;

import pousada.domain.DomainException;
import pousada.domain.Guest;
import pousada.domain.RatePlan;

import java.time.LocalDate;

public record NewReservation(int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut,
                             int guests, RatePlan ratePlan) {
    public NewReservation {
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída precisa ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Code.INVALID_REQUEST, message);
    }
}
