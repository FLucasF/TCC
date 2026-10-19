package com.pousadamareaalta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final int SCALE = 2;

    public BigDecimal calculateNightPrice(BigDecimal nightlyRate, LocalDate date) {
        if (isHighSeason(date)) {
            return nightlyRate.multiply(SEASON_MULTIPLIER)
                .setScale(SCALE, RoundingMode.HALF_EVEN);
        }
        return nightlyRate.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateSubtotal(BigDecimal[] nightPrices) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : nightPrices) {
            sum = sum.add(price);
        }
        return sum.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (isNonRefundable) {
            return subtotal.multiply(DISCOUNT_RATE)
                .setScale(SCALE, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount)
            .setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month == 1 || month == 2) return true;

        return false;
    }
}
