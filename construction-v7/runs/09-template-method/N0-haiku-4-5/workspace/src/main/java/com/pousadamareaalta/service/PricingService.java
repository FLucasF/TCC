package com.pousadamareaalta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            if (month == Month.FEBRUARY && day > 28) {
                return false;
            }
            return true;
        }

        return false;
    }

    public BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(price);
    }

    public BigDecimal calculateSubtotal(BigDecimal nightlyPrice, long nights) {
        BigDecimal subtotal = nightlyPrice.multiply(new BigDecimal(nights));
        return round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
        return round(discount);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal total = subtotal.subtract(discount);
        return round(total);
    }

    public BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
