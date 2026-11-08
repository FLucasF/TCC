package com.marealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // July
        if (month == 7) return true;

        // December 15 to February 28/29
        if (month == 12 && day >= 15) return true;
        if (month == 1) return true;
        if (month == 2) return true;

        return false;
    }

    public static BigDecimal getNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, ROUNDING_MODE);
        }
        return baseRate;
    }

    public static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }

    public static PricingResult calculatePrice(LocalDate checkIn, LocalDate checkOut,
                                                BigDecimal baseNightlyRate, String ratePlan,
                                                String companyCode) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        if (ratePlan.equals("WEEKEND_PACKAGE")) {
            return calculateWeekendPackage(checkIn, checkOut, baseNightlyRate);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < nights; i++) {
            LocalDate currentDate = checkIn.plusDays(i);
            BigDecimal rate = getNightlyRate(baseNightlyRate, currentDate);
            subtotal = subtotal.add(rate);
        }
        subtotal = roundMoney(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan.equals("NON_REFUNDABLE")) {
            discount = subtotal.multiply(new BigDecimal("0.15"));
        } else if (ratePlan.equals("CORPORATE")) {
            BigDecimal discountRate = getCorporateDiscountRate(companyCode);
            discount = subtotal.multiply(discountRate);
        } else if (ratePlan.equals("LONG_STAY")) {
            discount = subtotal.multiply(new BigDecimal("0.10"));
        }
        discount = roundMoney(discount);

        BigDecimal total = subtotal.subtract(discount);
        total = roundMoney(total);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private static BigDecimal getCorporateDiscountRate(String companyCode) {
        return switch (companyCode) {
            case "TECNOSUL" -> new BigDecimal("0.12");
            case "AGROVALE" -> new BigDecimal("0.18");
            case "FARMAVIDA" -> new BigDecimal("0.25");
            default -> BigDecimal.ZERO;
        };
    }

    private static PricingResult calculateWeekendPackage(LocalDate checkIn, LocalDate checkOut,
                                                          BigDecimal baseNightlyRate) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal packagePrice = switch (nights) {
            case 2 -> new BigDecimal("520.00");
            default -> new BigDecimal("820.00");
        };

        // This will be determined by room type in the controller
        BigDecimal subtotal = packagePrice;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = packagePrice;

        return new PricingResult(nights, subtotal, discount, total);
    }

    public static class PricingResult {
        public int nights;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
