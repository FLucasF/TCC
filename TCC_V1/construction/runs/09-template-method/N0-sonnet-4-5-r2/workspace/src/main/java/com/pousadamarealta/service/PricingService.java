package com.pousadamarealta.service;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(Room room, LocalDate date) {
        BigDecimal basePrice = room.getNightlyRate();

        if (DateUtils.isHighSeason(date)) {
            basePrice = MoneyUtils.round(basePrice.multiply(HIGH_SEASON_MULTIPLIER));
        }

        return basePrice;
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, currentDate);
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
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

    public BigDecimal calculateTourismTax(int guests, int nights) {
        BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");
        BigDecimal totalGuests = new BigDecimal(guests);
        BigDecimal totalNights = new BigDecimal(nights);
        return MoneyUtils.round(taxPerGuestPerNight.multiply(totalGuests).multiply(totalNights));
    }
}
