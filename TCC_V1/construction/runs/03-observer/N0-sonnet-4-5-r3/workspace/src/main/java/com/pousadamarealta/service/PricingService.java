package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;

        if (isHighSeason(night)) {
            price = MoneyUtil.round(price.multiply(HIGH_SEASON_MULTIPLIER));
        }

        return price;
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return MoneyUtil.round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return MoneyUtil.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return MoneyUtil.round(subtotal.subtract(discount));
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
