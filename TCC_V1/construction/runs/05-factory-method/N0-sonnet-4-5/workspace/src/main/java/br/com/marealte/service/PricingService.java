package br.com.marealte.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {

    public static boolean isHighSeason(LocalDate date) {
        int year = date.getYear();
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // 15 de dezembro até fim de fevereiro
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(new BigDecimal("1.20"));
        }
        return round(price);
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
