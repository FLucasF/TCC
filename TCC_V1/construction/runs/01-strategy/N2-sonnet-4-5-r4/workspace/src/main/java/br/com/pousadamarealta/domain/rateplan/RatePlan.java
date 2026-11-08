package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.time.LocalDate;

public interface RatePlan {
    void validate(ReservationRequest request, Room room);
    PriceCalculation calculatePrice(ReservationRequest request, Room room);
    java.math.BigDecimal calculateRefund(Reservation reservation);

    record ReservationRequest(
        String roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String companyCode
    ) {}
}
