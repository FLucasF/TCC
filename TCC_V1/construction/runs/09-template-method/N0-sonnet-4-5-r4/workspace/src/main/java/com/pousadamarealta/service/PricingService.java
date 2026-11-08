package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundBankers(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER.getValue() && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY.getValue() || month == Month.FEBRUARY.getValue()) {
            return true;
        }

        if (month == Month.JULY.getValue()) {
            return true;
        }

        return false;
    }

    public BigDecimal roundBankers(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
