package br.com.pousadamarealta.domain.pricing;

import br.com.pousadamarealta.domain.reservation.RatePlan;
import br.com.pousadamarealta.domain.room.Room;
import br.com.pousadamarealta.util.DateUtils;
import br.com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public static BigDecimal calculateNightlyRate(Room room, LocalDate date) {
        BigDecimal baseRate = room.getNightlyRate();
        if (DateUtils.isHighSeason(date)) {
            return MoneyUtils.round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(room, current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return MoneyUtils.round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return MoneyUtils.round(discount);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return MoneyUtils.round(subtotal.subtract(discount));
    }
}
