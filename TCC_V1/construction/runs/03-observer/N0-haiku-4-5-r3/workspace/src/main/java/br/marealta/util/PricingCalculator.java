package br.marealta.util;

import br.marealta.model.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.DECEMBER && dayOfMonth >= 15) return true;
        if (month == Month.JANUARY) return true;
        if (month == Month.FEBRUARY) return true;
        if (month == Month.JULY) return true;

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal nightlyRate, LocalDate night) {
        BigDecimal price = nightlyRate;

        if (isHighSeason(night)) {
            price = price.multiply(new BigDecimal("1.20"));
        }

        return MoneyRounder.round(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightPrice(nightlyRate, current));
            current = current.plusDays(1);
        }

        return MoneyRounder.round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"));
            return MoneyRounder.round(discount);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal total = subtotal.subtract(discount);
        return MoneyRounder.round(total);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
