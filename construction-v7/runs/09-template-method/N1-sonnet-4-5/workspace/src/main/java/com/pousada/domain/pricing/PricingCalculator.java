package com.pousada.domain.pricing;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static BigDecimal calculateNightPrice(RoomType roomType, LocalDate night) {
        BigDecimal basePrice = roomType.getNightlyRate();

        if (SeasonCalculator.isHighSeason(night)) {
            basePrice = basePrice.multiply(HIGH_SEASON_MULTIPLIER);
        }

        return roundHalfToEven(basePrice);
    }

    public static BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(roomType, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return roundHalfToEven(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return roundHalfToEven(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundHalfToEven(subtotal.subtract(discount));
    }

    public static BigDecimal calculateTourismTax(int guests, int nights) {
        BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");
        return taxPerGuestPerNight.multiply(BigDecimal.valueOf(guests))
                                  .multiply(BigDecimal.valueOf(nights));
    }

    private static BigDecimal roundHalfToEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
