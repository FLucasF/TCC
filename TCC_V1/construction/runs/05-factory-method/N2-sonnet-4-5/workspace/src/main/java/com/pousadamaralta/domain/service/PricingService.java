package com.pousadamaralta.domain.service;

import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.domain.model.RatePlanType;
import com.pousadamaralta.domain.rateplan.RatePlan;
import com.pousadamaralta.domain.rateplan.RatePlanFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public BigDecimal calculateNightPrice(Room room, LocalDate night) {
        BigDecimal basePrice = room.getNightlyRate();

        if (isHighSeason(night)) {
            return basePrice.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }

        return basePrice;
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

    public BigDecimal calculateDiscount(RatePlanType ratePlanType, BigDecimal subtotal) {
        RatePlan plan = RatePlanFactory.get(ratePlanType);
        return plan.calculateDiscount(subtotal);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateRefund(RatePlanType ratePlanType, BigDecimal amountPaid, BigDecimal total) {
        RatePlan plan = RatePlanFactory.get(ratePlanType);
        return plan.calculateRefund(amountPaid, total);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        return false;
    }

    public int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut).getDays();
    }
}
