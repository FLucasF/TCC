package com.pousada.domain.service;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public final class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceBreakdown {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceBreakdown(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public PriceBreakdown calculate(RoomType roomType, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(roomType, checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);

        return new PriceBreakdown(nights, subtotal, discount, total);
    }

    private int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut).getDays();
    }

    private BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(roomType, current);
            sum = sum.add(nightPrice);
            current = current.plusDays(1);
        }

        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateNightPrice(RoomType roomType, LocalDate date) {
        BigDecimal basePrice = roomType.nightlyRate();

        if (isHighSeason(date)) {
            return basePrice.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }

        return basePrice;
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

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }
}
