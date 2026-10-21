package com.pousada.rateplan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public abstract class BaseRatePlanStrategy implements RatePlanStrategy {

    protected BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
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

    protected BigDecimal calculateNightRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate.multiply(new BigDecimal("1.20")));
        }
        return baseRate;
    }

    protected BigDecimal calculateNightlySubtotal(PricingContext context) {
        BigDecimal baseRate = context.getRoomType().getNightlyRate();
        BigDecimal total = BigDecimal.ZERO;

        LocalDate current = context.getCheckIn();
        while (current.isBefore(context.getCheckOut())) {
            total = total.add(calculateNightRate(baseRate, current));
            current = current.plusDays(1);
        }

        return round(total);
    }
}
