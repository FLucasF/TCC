package com.pousadamarealta.util;

import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PriceCalculator {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public static BigDecimal calculateRegularSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = room.getNightlyRate();
            if (isHighSeason(current)) {
                nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER)
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
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

    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
