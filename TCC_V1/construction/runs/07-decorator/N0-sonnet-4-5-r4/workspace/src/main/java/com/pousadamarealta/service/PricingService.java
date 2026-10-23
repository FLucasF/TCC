package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public BigDecimal calculateNightlyRate(RoomType roomType, LocalDate date) {
        BigDecimal baseRate = roomType.getNightlyRate();

        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_EVEN);
        }

        return baseRate;
    }

    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(roomType, currentDate);
            total = total.add(nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discountAmount = subtotal.multiply(ratePlan.getDiscountRate());
        return discountAmount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
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
