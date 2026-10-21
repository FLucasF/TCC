package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public final class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public record PricingResult(
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {}

    public PricingResult calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = (int) checkIn.datesUntil(checkOut).count();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightRate = room.nightlyRate();
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(HIGH_SEASON_MULTIPLIER);
            }
            nightRate = round(nightRate);
            subtotal = subtotal.add(nightRate);
        }

        subtotal = round(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }

        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(nights, subtotal, discount, total);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) return true;
        if (month == Month.JANUARY || month == Month.FEBRUARY) return true;
        if (month == Month.JULY) return true;

        return false;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
