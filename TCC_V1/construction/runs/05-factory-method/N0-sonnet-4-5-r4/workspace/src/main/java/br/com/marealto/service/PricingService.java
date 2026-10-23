package br.com.marealto.service;

import br.com.marealto.model.RatePlan;
import br.com.marealto.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        if (month == 7) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateNightPrice(Room room, LocalDate date) {
        BigDecimal basePrice = room.getNightlyRate();

        if (isHighSeason(date)) {
            basePrice = basePrice.multiply(HIGH_SEASON_MULTIPLIER);
        }

        return basePrice.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, currentDate);
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateIss(BigDecimal total) {
        return total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }
}
