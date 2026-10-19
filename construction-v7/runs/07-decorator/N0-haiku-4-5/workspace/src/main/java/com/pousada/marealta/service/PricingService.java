package com.pousada.marealta.service;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.util.DateUtil;
import com.pousada.marealta.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceCalculation {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceCalculation(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceCalculation calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal nightlyRate = room.nightlyRate();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            BigDecimal nightRate = nightlyRate;
            if (DateUtil.isHighSeason(date)) {
                nightRate = nightRate.multiply(HIGH_SEASON_MULTIPLIER);
            }
            nightRate = MoneyUtil.roundHalfEven(nightRate);
            subtotal = subtotal.add(nightRate);
        }

        subtotal = MoneyUtil.roundHalfEven(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
            discount = MoneyUtil.roundHalfEven(discount);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = MoneyUtil.roundHalfEven(total);

        return new PriceCalculation(nights, subtotal, discount, total);
    }
}
