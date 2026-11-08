package com.pousadamareaita.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    private static final Map<Integer, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
        101, new BigDecimal("520.00"),
        102, new BigDecimal("520.00"),
        201, new BigDecimal("820.00"),
        202, new BigDecimal("820.00"),
        301, new BigDecimal("1200.00")
    );

    public static class PricingResult {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PricingResult calculatePrice(int roomNumber, LocalDate checkIn, LocalDate checkOut,
                                               String ratePlan, String companyCode, BigDecimal nightlyRate) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        if ("WEEKEND_PACKAGE".equals(ratePlan)) {
            return calculateWeekendPackage(roomNumber, nights);
        }

        BigDecimal subtotal = calculateSubtotal(checkIn, checkOut, nightlyRate, ratePlan);

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT)
                .setScale(2, RoundingMode.HALF_EVEN);
        } else if ("CORPORATE".equals(ratePlan)) {
            BigDecimal discountRate = CORPORATE_DISCOUNTS.getOrDefault(companyCode, BigDecimal.ZERO);
            discount = subtotal.multiply(discountRate)
                .setScale(2, RoundingMode.HALF_EVEN);
        } else if ("LONG_STAY".equals(ratePlan)) {
            discount = subtotal.multiply(LONG_STAY_DISCOUNT)
                .setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal total = subtotal.subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private static PricingResult calculateWeekendPackage(int roomNumber, int nights) {
        BigDecimal price = WEEKEND_PACKAGE_PRICES.get(roomNumber);
        if (price == null) {
            throw new IllegalArgumentException("Room not found for weekend package");
        }
        return new PricingResult(nights, price, BigDecimal.ZERO, price);
    }

    private static BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut,
                                                BigDecimal nightlyRate, String ratePlan) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal dayRate = nightlyRate;

            if (isHighSeason(current)) {
                dayRate = nightlyRate.multiply(HIGH_SEASON_MULTIPLIER);
            }

            total = total.add(dayRate.setScale(2, RoundingMode.HALF_EVEN));
            current = current.plusDays(1);
        }

        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        return (month == 7) ||
               (month == 12) ||
               (month == 1) ||
               (month == 2);
    }

    public static BigDecimal calculateRefund(String ratePlan, BigDecimal amountPaid) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return BigDecimal.ZERO;
        } else if ("WEEKEND_PACKAGE".equals(ratePlan)) {
            return amountPaid.multiply(new BigDecimal("0.5"))
                .setScale(2, RoundingMode.HALF_EVEN);
        } else {
            return amountPaid;
        }
    }
}
