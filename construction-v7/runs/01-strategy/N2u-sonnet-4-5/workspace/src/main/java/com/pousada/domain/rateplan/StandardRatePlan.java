package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.ValidationResult;
import com.pousada.util.DateUtils;
import com.pousada.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {

    @Override
    public ValidationResult validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        return ValidationResult.valid();
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, nights);
        BigDecimal discount = MoneyUtils.roundMoney(BigDecimal.ZERO);
        BigDecimal total = subtotal;

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }

    protected BigDecimal calculateSubtotal(Room room, LocalDate checkIn, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightRate = room.getNightlyRate();

            if (DateUtils.isHighSeason(night)) {
                nightRate = MoneyUtils.multiply(nightRate, new BigDecimal("1.20"));
            } else {
                nightRate = MoneyUtils.roundMoney(nightRate);
            }

            subtotal = subtotal.add(nightRate);
        }

        return MoneyUtils.roundMoney(subtotal);
    }
}
