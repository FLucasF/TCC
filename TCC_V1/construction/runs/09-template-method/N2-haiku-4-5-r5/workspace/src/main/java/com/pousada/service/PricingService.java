package com.pousada.service;

import com.pousada.domain.Money;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final int NON_REFUNDABLE_DISCOUNT_PERCENT = 15;

    public static class PricingResult {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = room.nightlyRate;
            if (SeasonalityCalculator.isHighSeason(date)) {
                nightlyRate = Money.multiply(nightlyRate, HIGH_SEASON_MULTIPLIER);
            }
            subtotal = subtotal.add(nightlyRate);
        }

        subtotal = Money.round(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = Money.percent(subtotal, NON_REFUNDABLE_DISCOUNT_PERCENT);
        }

        BigDecimal total = Money.round(subtotal.subtract(discount));

        return new PricingResult(nights, subtotal, discount, total);
    }

    private static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
