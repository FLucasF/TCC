package com.pousada.service;

import com.pousada.util.PriceCalculator;
import com.pousada.util.SeasonUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;

public class RatePlanCalculator {
    public static class PricingResult {
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;
        public int nights;

        public PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total, int nights) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.nights = nights;
        }
    }

    public static PricingResult calculate(String ratePlan, int roomNumber, BigDecimal nightlyRate,
                                         LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        return switch (ratePlan) {
            case "STANDARD" -> calculateStandard(nightlyRate, checkIn, checkOut);
            case "NON_REFUNDABLE" -> calculateNonRefundable(nightlyRate, checkIn, checkOut);
            case "CORPORATE" -> calculateCorporate(nightlyRate, checkIn, checkOut, companyCode);
            case "WEEKEND_PACKAGE" -> calculateWeekendPackage(roomNumber, checkIn, checkOut);
            case "LONG_STAY" -> calculateLongStay(nightlyRate, checkIn, checkOut);
            default -> throw new IllegalArgumentException("Unknown rate plan: " + ratePlan);
        };
    }

    private static PricingResult calculateStandard(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal rate = nightlyRate;
            if (SeasonUtils.isHighSeason(date)) {
                rate = PriceCalculator.multiply(rate, new BigDecimal("1.20"));
            }
            subtotal = PriceCalculator.add(subtotal, rate);
        }

        return new PricingResult(subtotal, BigDecimal.ZERO, subtotal, nights);
    }

    private static PricingResult calculateNonRefundable(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal rate = nightlyRate;
            if (SeasonUtils.isHighSeason(date)) {
                rate = PriceCalculator.multiply(rate, new BigDecimal("1.20"));
            }
            subtotal = PriceCalculator.add(subtotal, rate);
        }

        BigDecimal discount = PriceCalculator.percentage(subtotal, 15);
        BigDecimal total = PriceCalculator.subtract(subtotal, discount);

        return new PricingResult(subtotal, discount, total, nights);
    }

    private static PricingResult calculateCorporate(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int discountPercent = getCompanyDiscount(companyCode);
        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal rate = nightlyRate;
            if (SeasonUtils.isHighSeason(date)) {
                rate = PriceCalculator.multiply(rate, new BigDecimal("1.20"));
            }
            subtotal = PriceCalculator.add(subtotal, rate);
        }

        BigDecimal discount = PriceCalculator.percentage(subtotal, discountPercent);
        BigDecimal total = PriceCalculator.subtract(subtotal, discount);

        return new PricingResult(subtotal, discount, total, nights);
    }

    private static PricingResult calculateWeekendPackage(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = getWeekendPackagePrice(roomNumber);
        int nights = (int) checkIn.until(checkOut).getDays();

        return new PricingResult(packagePrice, BigDecimal.ZERO, packagePrice, nights);
    }

    private static PricingResult calculateLongStay(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) checkIn.until(checkOut).getDays();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal rate = nightlyRate;
            if (SeasonUtils.isHighSeason(date)) {
                rate = PriceCalculator.multiply(rate, new BigDecimal("1.20"));
            }
            subtotal = PriceCalculator.add(subtotal, rate);
        }

        BigDecimal discount = PriceCalculator.percentage(subtotal, 10);
        BigDecimal total = PriceCalculator.subtract(subtotal, discount);

        return new PricingResult(subtotal, discount, total, nights);
    }

    private static int getCompanyDiscount(String companyCode) {
        return switch (companyCode) {
            case "TECNOSUL" -> 12;
            case "AGROVALE" -> 18;
            case "FARMAVIDA" -> 25;
            default -> throw new IllegalArgumentException("Unknown company code: " + companyCode);
        };
    }

    private static BigDecimal getWeekendPackagePrice(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> new BigDecimal("520.00");
            case 201, 202 -> new BigDecimal("820.00");
            case 301 -> new BigDecimal("1200.00");
            default -> throw new IllegalArgumentException("Unknown room: " + roomNumber);
        };
    }

    public static BigDecimal getRefundAmount(String ratePlan, BigDecimal amountPaid) {
        return switch (ratePlan) {
            case "STANDARD", "CORPORATE", "LONG_STAY" -> amountPaid;
            case "NON_REFUNDABLE" -> BigDecimal.ZERO;
            case "WEEKEND_PACKAGE" -> PriceCalculator.divide(amountPaid, new BigDecimal("2"));
            default -> throw new IllegalArgumentException("Unknown rate plan: " + ratePlan);
        };
    }
}
