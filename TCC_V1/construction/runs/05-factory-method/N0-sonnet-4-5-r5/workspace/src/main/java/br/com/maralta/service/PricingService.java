package br.com.maralta.service;

import br.com.maralta.model.RatePlan;
import br.com.maralta.model.Room;
import br.com.maralta.util.MoneyUtils;
import br.com.maralta.util.SeasonUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightlyRate(Room room, LocalDate night) {
        BigDecimal baseRate = room.getNightlyRate();

        if (SeasonUtils.isHighSeason(night)) {
            baseRate = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }

        return MoneyUtils.round(baseRate);
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(room, current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return MoneyUtils.round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return MoneyUtils.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return MoneyUtils.round(subtotal.subtract(discount));
    }
}
