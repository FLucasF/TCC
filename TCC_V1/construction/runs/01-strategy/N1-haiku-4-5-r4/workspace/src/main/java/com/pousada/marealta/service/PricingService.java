package com.pousada.marealta.service;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;
import com.pousada.marealta.util.MoneyRounder;
import com.pousada.marealta.util.SeasonChecker;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class PricingService {

    public static class PriceCalculation {
        public int nights;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceCalculation(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> calculateStandard(room, checkIn, checkOut);
            case NON_REFUNDABLE -> calculateNonRefundable(room, checkIn, checkOut);
            case CORPORATE -> calculateCorporate(room, checkIn, checkOut, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPackage(room, checkIn, checkOut);
            case LONG_STAY -> calculateLongStay(room, checkIn, checkOut);
        };
    }

    private PriceCalculation calculateStandard(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightPrice = room.getNightlyRate();
            if (SeasonChecker.isHighSeason(night)) {
                nightPrice = nightPrice.multiply(new BigDecimal("1.20"));
            }
            subtotal = subtotal.add(nightPrice);
        }

        subtotal = MoneyRounder.round(subtotal);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PriceCalculation(nights, subtotal, discount, total);
    }

    private PriceCalculation calculateNonRefundable(Room room, LocalDate checkIn, LocalDate checkOut) {
        PriceCalculation std = calculateStandard(room, checkIn, checkOut);
        BigDecimal discount = std.subtotal.multiply(new BigDecimal("0.15"));
        discount = MoneyRounder.round(discount);
        BigDecimal total = std.subtotal.subtract(discount);
        total = MoneyRounder.round(total);
        return new PriceCalculation(std.nights, std.subtotal, discount, total);
    }

    private PriceCalculation calculateCorporate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        PriceCalculation std = calculateStandard(room, checkIn, checkOut);

        BigDecimal discountRate = switch (companyCode) {
            case "TECNOSUL" -> new BigDecimal("0.12");
            case "AGROVALE" -> new BigDecimal("0.18");
            case "FARMAVIDA" -> new BigDecimal("0.25");
            default -> BigDecimal.ZERO;
        };

        BigDecimal discount = std.subtotal.multiply(discountRate);
        discount = MoneyRounder.round(discount);
        BigDecimal total = std.subtotal.subtract(discount);
        total = MoneyRounder.round(total);

        return new PriceCalculation(std.nights, std.subtotal, discount, total);
    }

    private PriceCalculation calculateWeekendPackage(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = switch (room.getType()) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };

        return new PriceCalculation(2, packagePrice, BigDecimal.ZERO, packagePrice);
    }

    private PriceCalculation calculateLongStay(Room room, LocalDate checkIn, LocalDate checkOut) {
        PriceCalculation std = calculateStandard(room, checkIn, checkOut);
        BigDecimal discount = std.subtotal.multiply(new BigDecimal("0.10"));
        discount = MoneyRounder.round(discount);
        BigDecimal total = std.subtotal.subtract(discount);
        total = MoneyRounder.round(total);
        return new PriceCalculation(std.nights, std.subtotal, discount, total);
    }

    public static boolean isWeekendPackageValid(LocalDate checkIn, LocalDate checkOut) {
        if (java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut) != 2) {
            return false;
        }

        DayOfWeek dayIn = checkIn.getDayOfWeek();
        DayOfWeek dayOut = checkOut.getDayOfWeek();

        return dayIn == DayOfWeek.FRIDAY && dayOut == DayOfWeek.SUNDAY;
    }

    public static BigDecimal calculateRefund(RatePlan ratePlan, BigDecimal amountPaid) {
        return switch (ratePlan) {
            case STANDARD -> amountPaid;
            case NON_REFUNDABLE -> BigDecimal.ZERO;
            case CORPORATE -> amountPaid;
            case WEEKEND_PACKAGE -> MoneyRounder.round(amountPaid.multiply(new BigDecimal("0.5")));
            case LONG_STAY -> amountPaid;
        };
    }
}
