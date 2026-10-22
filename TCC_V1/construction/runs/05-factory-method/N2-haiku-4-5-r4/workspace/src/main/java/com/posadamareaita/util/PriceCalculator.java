package com.posadamareaita.util;

import com.posadamareaita.domain.RatePlan;
import com.posadamareaita.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceResult {
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        BigDecimal nightlyRate = room.getNightlyRate();
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        // Calculate price per night considering season
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightPrice = nightlyRate;
            if (SeasonUtil.isHighSeason(date)) {
                nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER);
            }
            nightPrice = MoneyUtil.round(nightPrice);
            subtotal = subtotal.add(nightPrice);
        }
        subtotal = MoneyUtil.round(subtotal);

        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
            discount = MoneyUtil.round(discount);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = MoneyUtil.round(total);

        return new PriceResult(subtotal, discount, total);
    }
}
