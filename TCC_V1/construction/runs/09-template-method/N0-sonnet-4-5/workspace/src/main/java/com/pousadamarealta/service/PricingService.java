package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) {
            return true;
        }
        if (month == 1 || month == 2) {
            return true;
        }
        if (month == 7) {
            return true;
        }
        return false;
    }

    public BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
