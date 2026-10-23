package br.com.marealta.service;

import br.com.marealta.model.RatePlan;
import br.com.marealta.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public BigDecimal calculateNightPrice(Room room, LocalDate date) {
        BigDecimal basePrice = room.getNightlyRate();
        if (isHighSeason(date)) {
            basePrice = basePrice.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return basePrice.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateIss(BigDecimal total) {
        return total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
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
}
