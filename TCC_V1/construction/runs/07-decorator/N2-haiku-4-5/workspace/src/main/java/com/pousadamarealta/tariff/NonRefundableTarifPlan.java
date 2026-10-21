package com.pousadamarealta.tariff;

import com.pousadamarealta.util.DateUtil;
import com.pousadamarealta.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableTarifPlan implements TarifPlan {
    @Override
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal rate = nightlyRate;
            if (DateUtil.isHighSeason(night)) {
                rate = Money.multiply(rate, new BigDecimal("1.20"));
            }
            subtotal = Money.add(subtotal, rate);
        }
        return subtotal;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return Money.percent(subtotal, 15);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
