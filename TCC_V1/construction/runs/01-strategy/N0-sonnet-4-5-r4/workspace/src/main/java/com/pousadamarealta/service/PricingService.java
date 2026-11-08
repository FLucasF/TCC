package com.pousadamarealta.service;

import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public BigDecimal calculateNightlyRate(RoomType roomType, LocalDate date) {
        BigDecimal baseRate = roomType.getNightlyRate();
        if (isHighSeason(date)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    public BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }
}
