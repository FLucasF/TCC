package com.pousada.domain.rateplans;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends BaseRatePlan {

    @Override
    public String getId() {
        return "LONG_STAY";
    }

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        validateBasic(guests);
        int nights = calculateNights(checkIn, checkOut);
        if (nights < 7) {
            throw new InvalidReservationException("INVALID_REQUEST", "LONG_STAY requer no mínimo 7 noites");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = Money.percentOf(subtotal, 10);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
