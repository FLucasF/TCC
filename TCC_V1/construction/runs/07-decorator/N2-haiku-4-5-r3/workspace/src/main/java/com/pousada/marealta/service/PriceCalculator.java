package com.pousada.marealta.service;

import com.pousada.marealta.domain.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private final PricingStrategy pricingStrategy;

    public PriceCalculator(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static boolean isHighSeason(LocalDate date) {
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

    public PriceBreakdown calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = pricingStrategy.calculateNightRate(room, current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }
        subtotal = round(subtotal);

        BigDecimal discount = pricingStrategy.calculateDiscount(subtotal);
        discount = round(discount);

        BigDecimal total = round(subtotal.subtract(discount));

        return new PriceBreakdown(nights, subtotal, discount, total);
    }

    public static class PriceBreakdown {
        public int nights;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceBreakdown(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
