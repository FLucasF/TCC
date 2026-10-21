package com.pousadamarealta.service;

import com.pousadamarealta.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    private static final int LONG_STAY_MIN_NIGHTS = 7;

    public static class PriceBreakdown {
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceBreakdown(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public PriceBreakdown calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut,
                                        RatePlan ratePlan, String companyCode) {
        int nights = (int) checkIn.until(checkOut).getDays();

        if (ratePlan == RatePlan.WEEKEND_PACKAGE) {
            BigDecimal packagePrice = getWeekendPackagePrice(room.getType());
            return new PriceBreakdown(packagePrice, round(BigDecimal.ZERO), packagePrice);
        }

        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan, companyCode);
        BigDecimal total = round(subtotal.subtract(discount));

        return new PriceBreakdown(subtotal, discount, total);
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();
            if (isHighSeason(current)) {
                nightRate = round(nightRate.multiply(HIGH_SEASON_MULTIPLIER));
            }
            total = total.add(nightRate);
            current = current.plusDays(1);
        }

        return round(total);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan, String companyCode) {
        BigDecimal discountRate = switch (ratePlan) {
            case NON_REFUNDABLE -> NON_REFUNDABLE_DISCOUNT;
            case CORPORATE -> {
                Company company = Company.fromCode(companyCode);
                yield company != null ? company.getDiscountRate() : BigDecimal.ZERO;
            }
            case LONG_STAY -> LONG_STAY_DISCOUNT;
            default -> BigDecimal.ZERO;
        };

        return round(subtotal.multiply(discountRate));
    }

    private BigDecimal getWeekendPackagePrice(RoomType roomType) {
        return switch (roomType) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // Janeiro e fevereiro inteiros
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        // 15 de dezembro em diante
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateRefund(BigDecimal amountPaid, RatePlan ratePlan) {
        BigDecimal refundAmount = switch (ratePlan) {
            case STANDARD, CORPORATE, LONG_STAY -> amountPaid;
            case WEEKEND_PACKAGE -> round(amountPaid.multiply(new BigDecimal("0.50")));
            case NON_REFUNDABLE -> BigDecimal.ZERO;
        };

        return round(refundAmount);
    }

    public void validateRatePlan(RatePlan ratePlan, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = (int) checkIn.until(checkOut).getDays();

        switch (ratePlan) {
            case CORPORATE -> {
                if (Company.fromCode(companyCode) == null) {
                    throw new IllegalArgumentException("Invalid company code for corporate rate plan");
                }
            }
            case WEEKEND_PACKAGE -> {
                if (nights != 2) {
                    throw new IllegalArgumentException("Weekend package requires exactly 2 nights");
                }
                if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
                    throw new IllegalArgumentException("Weekend package must start on Friday");
                }
            }
            case LONG_STAY -> {
                if (nights < LONG_STAY_MIN_NIGHTS) {
                    throw new IllegalArgumentException("Long stay requires at least 7 nights");
                }
            }
        }
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
