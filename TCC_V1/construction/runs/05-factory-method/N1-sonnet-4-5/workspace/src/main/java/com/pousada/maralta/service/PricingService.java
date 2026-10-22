package com.pousada.maralta.service;

import com.pousada.maralta.domain.RatePlan;
import com.pousada.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(RoomType roomType, LocalDate night) {
        BigDecimal basePrice = roomType.getNightlyRate();
        if (isHighSeason(night)) {
            basePrice = basePrice.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(basePrice);
    }

    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            total = total.add(calculateNightPrice(roomType, current));
            current = current.plusDays(1);
        }
        return round(total);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    private boolean isHighSeason(LocalDate date) {
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

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
