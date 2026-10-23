package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PricingService {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            if (isHighSeason(currentDate)) {
                nightlyRate = nightlyRate.multiply(HIGH_SEASON_MULTIPLIER);
            }
            nightlyRate = round(nightlyRate);
            subtotal = subtotal.add(nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        return round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return round(discount);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
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

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
