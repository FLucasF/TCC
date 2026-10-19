package com.pousadamarealta.service;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public interface RatePlanStrategy {

    void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    default boolean isHighSeason(LocalDate date) {
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

    default BigDecimal calculateNightRate(RoomType roomType, LocalDate date) {
        BigDecimal baseRate = roomType.getNightlyRate();
        if (isHighSeason(date)) {
            return round(baseRate.multiply(new BigDecimal("1.20")));
        }
        return baseRate;
    }

    default BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
