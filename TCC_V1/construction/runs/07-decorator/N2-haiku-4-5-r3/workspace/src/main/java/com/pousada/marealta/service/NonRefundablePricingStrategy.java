package com.pousada.marealta.service;

import com.pousada.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundablePricingStrategy implements PricingStrategy {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public BigDecimal calculateNightRate(Room room, LocalDate date) {
        BigDecimal baseRate = room.getNightlyRate();
        if (PriceCalculator.isHighSeason(date)) {
            baseRate = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return PriceCalculator.round(baseRate);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(DISCOUNT_RATE);
    }
}
