package com.pousada.marealta.service;

import com.pousada.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardPricingStrategy implements PricingStrategy {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    @Override
    public BigDecimal calculateNightRate(Room room, LocalDate date) {
        BigDecimal baseRate = room.getNightlyRate();
        if (PriceCalculator.isHighSeason(date)) {
            return PriceCalculator.round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }
}
