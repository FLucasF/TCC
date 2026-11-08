package com.pousadamarealta.rateplans;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class SeasonalHelper {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER) {
            return date.getDayOfMonth() >= 15;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static BigDecimal applySeasonalRate(BigDecimal nightlyRate, LocalDate date) {
        if (isHighSeason(date)) {
            return nightlyRate.multiply(new BigDecimal("1.20"));
        }
        return nightlyRate;
    }

    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
