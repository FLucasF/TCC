package br.com.pousada.util;

import br.com.pousada.domain.RatePlan;
import br.com.pousada.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public static BigDecimal calculateNightPrice(Room room, LocalDate night) {
        BigDecimal baseRate = room.getNightlyRate();
        if (SeasonUtil.isHighSeason(night)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateISS(BigDecimal total) {
        return total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }
}
