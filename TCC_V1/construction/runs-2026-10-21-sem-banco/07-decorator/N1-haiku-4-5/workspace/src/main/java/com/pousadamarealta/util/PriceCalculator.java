package com.pousadamarealta.util;

import com.pousadamarealta.domain.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = BigDecimal.valueOf(0.15);

    public static PriceBreakdown calculate(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = DateUtil.countNights(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = nightlyRate;
            if (DateUtil.isHighSeason(current)) {
                nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER);
            }
            nightPrice = MoneyUtil.round(nightPrice);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        subtotal = MoneyUtil.round(subtotal);

        BigDecimal discount;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT);
            discount = MoneyUtil.round(discount);
        } else {
            discount = MoneyUtil.round(BigDecimal.ZERO);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = MoneyUtil.round(total);

        return new PriceBreakdown(nights, subtotal, discount, total);
    }

    public static class PriceBreakdown {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceBreakdown(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
