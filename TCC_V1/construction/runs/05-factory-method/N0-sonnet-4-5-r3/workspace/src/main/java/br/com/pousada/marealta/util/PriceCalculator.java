package br.com.pousada.marealta.util;

import br.com.pousada.marealta.model.RatePlan;
import br.com.pousada.marealta.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightPrice(RoomType roomType, LocalDate date) {
        BigDecimal basePrice = roomType.getNightlyRate();
        if (isHighSeason(date)) {
            return basePrice.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        return basePrice;
    }

    public static BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(roomType, currentDate);
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
