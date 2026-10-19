package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.ValidationResult;
import com.pousada.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends StandardRatePlan {

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, nights);
        BigDecimal discount = MoneyUtils.percentage(subtotal, 15);
        BigDecimal total = MoneyUtils.roundMoney(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
