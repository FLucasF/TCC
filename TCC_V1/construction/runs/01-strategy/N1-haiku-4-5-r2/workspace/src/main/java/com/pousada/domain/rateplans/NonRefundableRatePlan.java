package com.pousada.domain.rateplans;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends BaseRatePlan {

    @Override
    public String getId() {
        return "NON_REFUNDABLE";
    }

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        validateBasic(guests);
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = Money.percentOf(subtotal, 15);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
