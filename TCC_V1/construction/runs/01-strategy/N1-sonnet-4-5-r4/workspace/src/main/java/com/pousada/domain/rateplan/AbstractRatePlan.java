package com.pousada.domain.rateplan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public abstract class AbstractRatePlan implements RatePlan {

    protected BigDecimal roundMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    protected boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    protected BigDecimal calculateNightPrice(BigDecimal nightlyRate, LocalDate night) {
        BigDecimal price = nightlyRate;
        if (isHighSeason(night)) {
            price = price.multiply(new BigDecimal("1.20"));
        }
        return roundMoney(price);
    }

    protected int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
