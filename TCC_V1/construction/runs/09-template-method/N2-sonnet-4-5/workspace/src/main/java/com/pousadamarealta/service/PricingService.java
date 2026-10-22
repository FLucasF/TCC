package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(Room room, LocalDate date) {
        BigDecimal basePrice = room.getNightlyRate();
        if (isHighSeason(date)) {
            return basePrice.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return basePrice;
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, date);
            subtotal = subtotal.add(nightPrice);
            date = date.plusDays(1);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
