package com.pousada.service;

import com.pousada.domain.rateplan.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final int HIGH_SEASON_START_MONTH = 12;
    private static final int HIGH_SEASON_START_DAY = 15;
    private static final int HIGH_SEASON_END_MONTH = 2;
    private static final int HIGH_SEASON_JULY_MONTH = 7;

    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightPrice = nightlyRate;
            if (isHighSeason(currentDate)) {
                nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        return ratePlan.calculateDiscount(subtotal);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == HIGH_SEASON_JULY_MONTH) {
            return true;
        }

        if (month == HIGH_SEASON_START_MONTH && day >= HIGH_SEASON_START_DAY) {
            return true;
        }

        if (month == 1 || month == HIGH_SEASON_END_MONTH) {
            return true;
        }

        return false;
    }
}
