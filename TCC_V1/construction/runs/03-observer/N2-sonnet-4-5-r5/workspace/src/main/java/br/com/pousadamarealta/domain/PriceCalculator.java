package br.com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public final class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static BigDecimal calculateNightPrice(LocalDate date, BigDecimal baseRate) {
        BigDecimal rate = baseRate;
        if (isHighSeason(date)) {
            rate = rate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return rate.setScale(2, ROUNDING);
    }

    public static BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, BigDecimal baseRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightPrice(date, baseRate));
            date = date.plusDays(1);
        }
        return subtotal.setScale(2, ROUNDING);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.discountRate());
        return discount.setScale(2, ROUNDING);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, ROUNDING);
    }

    private static boolean isHighSeason(LocalDate date) {
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
}
