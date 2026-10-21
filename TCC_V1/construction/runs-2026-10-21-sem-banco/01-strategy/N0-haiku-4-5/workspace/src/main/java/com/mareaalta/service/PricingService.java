package com.mareaalta.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static double roundToTwoCents(double value) {
        return new BigDecimal(value).setScale(2, ROUNDING_MODE).doubleValue();
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static int calculateNights(String checkInStr, String checkOutStr) {
        LocalDate checkIn = LocalDate.parse(checkInStr, DATE_FORMAT);
        LocalDate checkOut = LocalDate.parse(checkOutStr, DATE_FORMAT);
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static PricingResult calculatePrice(String ratePlan, int nights, double nightlyRate, String checkIn, String checkOut, String companyCode) {
        return switch (ratePlan) {
            case "STANDARD" -> calculateStandard(nights, nightlyRate, checkIn);
            case "NON_REFUNDABLE" -> calculateNonRefundable(nights, nightlyRate, checkIn);
            case "CORPORATE" -> calculateCorporate(nights, nightlyRate, checkIn, companyCode);
            case "WEEKEND_PACKAGE" -> calculateWeekendPackage(nights, nightlyRate, checkIn, checkOut);
            case "LONG_STAY" -> calculateLongStay(nights, nightlyRate, checkIn);
            default -> null;
        };
    }

    private static PricingResult calculateStandard(int nights, double nightlyRate, String checkIn) {
        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        double subtotal = 0;
        for (int i = 0; i < nights; i++) {
            LocalDate date = checkInDate.plusDays(i);
            double rate = nightlyRate;
            if (isHighSeason(date)) {
                rate = roundToTwoCents(rate * 1.2);
            } else {
                rate = roundToTwoCents(rate);
            }
            subtotal = roundToTwoCents(subtotal + rate);
        }
        return new PricingResult(subtotal, 0, subtotal);
    }

    private static PricingResult calculateNonRefundable(int nights, double nightlyRate, String checkIn) {
        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        double subtotal = 0;
        for (int i = 0; i < nights; i++) {
            LocalDate date = checkInDate.plusDays(i);
            double rate = nightlyRate;
            if (isHighSeason(date)) {
                rate = roundToTwoCents(rate * 1.2);
            } else {
                rate = roundToTwoCents(rate);
            }
            subtotal = roundToTwoCents(subtotal + rate);
        }
        double discount = roundToTwoCents(subtotal * 0.15);
        double total = roundToTwoCents(subtotal - discount);
        return new PricingResult(subtotal, discount, total);
    }

    private static PricingResult calculateCorporate(int nights, double nightlyRate, String checkIn, String companyCode) {
        double discountRate = getCompanyDiscountRate(companyCode);
        if (discountRate < 0) return null;

        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        double subtotal = 0;
        for (int i = 0; i < nights; i++) {
            LocalDate date = checkInDate.plusDays(i);
            double rate = nightlyRate;
            if (isHighSeason(date)) {
                rate = roundToTwoCents(rate * 1.2);
            } else {
                rate = roundToTwoCents(rate);
            }
            subtotal = roundToTwoCents(subtotal + rate);
        }
        double discount = roundToTwoCents(subtotal * discountRate);
        double total = roundToTwoCents(subtotal - discount);
        return new PricingResult(subtotal, discount, total);
    }

    private static PricingResult calculateWeekendPackage(int nights, double nightlyRate, String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        LocalDate checkOutDate = LocalDate.parse(checkOut, DATE_FORMAT);

        int actualNights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (actualNights != 2) return null;

        if (checkInDate.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) return null;
        if (checkOutDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) return null;

        double packagePrice = getWeekendPackagePrice(nightlyRate);
        if (packagePrice < 0) return null;

        return new PricingResult(packagePrice, 0, packagePrice);
    }

    private static double getWeekendPackagePrice(double nightlyRate) {
        double rounded = Math.round(nightlyRate * 100.0) / 100.0;
        if (rounded == 249.90) return 520.00;
        if (rounded == 389.90) return 820.00;
        if (rounded == 579.90) return 1200.00;
        return -1;
    }

    private static PricingResult calculateLongStay(int nights, double nightlyRate, String checkIn) {
        if (nights < 7) return null;

        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        double subtotal = 0;
        for (int i = 0; i < nights; i++) {
            LocalDate date = checkInDate.plusDays(i);
            double rate = nightlyRate;
            if (isHighSeason(date)) {
                rate = roundToTwoCents(rate * 1.2);
            } else {
                rate = roundToTwoCents(rate);
            }
            subtotal = roundToTwoCents(subtotal + rate);
        }
        double discount = roundToTwoCents(subtotal * 0.10);
        double total = roundToTwoCents(subtotal - discount);
        return new PricingResult(subtotal, discount, total);
    }

    private static double getCompanyDiscountRate(String companyCode) {
        return switch (companyCode) {
            case "TECNOSUL" -> 0.12;
            case "AGROVALE" -> 0.18;
            case "FARMAVIDA" -> 0.25;
            default -> -1;
        };
    }

    public static class PricingResult {
        public double subtotal;
        public double discount;
        public double total;

        public PricingResult(double subtotal, double discount, double total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
