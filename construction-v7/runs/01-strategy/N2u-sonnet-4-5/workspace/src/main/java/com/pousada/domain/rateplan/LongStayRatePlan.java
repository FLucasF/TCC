package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.ValidationResult;
import com.pousada.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends StandardRatePlan {

    @Override
    public ValidationResult validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights < 7) {
            return ValidationResult.invalid("Long stay requires minimum 7 nights");
        }

        return ValidationResult.valid();
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, nights);
        BigDecimal discount = MoneyUtils.percentage(subtotal, 10);
        BigDecimal total = MoneyUtils.roundMoney(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
