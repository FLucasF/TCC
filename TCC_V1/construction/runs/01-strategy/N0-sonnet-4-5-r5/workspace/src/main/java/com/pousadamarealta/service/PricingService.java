package com.pousadamarealta.service;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

public class PricingService {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal WEEKEND_REFUND_RATE = new BigDecimal("0.50");

    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
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

    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut,
                                       RatePlan ratePlan, String companyCode) {
        int nights = (int) checkIn.until(checkOut).getDays();

        return switch (ratePlan) {
            case STANDARD -> calculateStandardPrice(room, checkIn, checkOut, nights);
            case NON_REFUNDABLE -> calculateNonRefundablePrice(room, checkIn, checkOut, nights);
            case CORPORATE -> calculateCorporatePrice(room, checkIn, checkOut, nights, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPackagePrice(room);
            case LONG_STAY -> calculateLongStayPrice(room, checkIn, checkOut, nights);
        };
    }

    private PricingResult calculateStandardPrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateNightlyTotal(room, checkIn, checkOut);
        return new PricingResult(nights, subtotal, BigDecimal.ZERO, subtotal);
    }

    private PricingResult calculateNonRefundablePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateNightlyTotal(room, checkIn, checkOut);
        BigDecimal discount = round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        BigDecimal total = subtotal.subtract(discount);
        return new PricingResult(nights, subtotal, discount, total);
    }

    private PricingResult calculateCorporatePrice(Room room, LocalDate checkIn, LocalDate checkOut,
                                                  int nights, String companyCode) {
        BigDecimal discountRate = CORPORATE_DISCOUNTS.get(companyCode);
        if (discountRate == null) {
            throw new IllegalArgumentException("Invalid company code");
        }

        BigDecimal subtotal = calculateNightlyTotal(room, checkIn, checkOut);
        BigDecimal discount = round(subtotal.multiply(discountRate));
        BigDecimal total = subtotal.subtract(discount);
        return new PricingResult(nights, subtotal, discount, total);
    }

    private PricingResult calculateWeekendPackagePrice(Room room) {
        BigDecimal packagePrice = room.getType().getWeekendPackagePrice();
        return new PricingResult(2, packagePrice, BigDecimal.ZERO, packagePrice);
    }

    private PricingResult calculateLongStayPrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = calculateNightlyTotal(room, checkIn, checkOut);
        BigDecimal discount = round(subtotal.multiply(LONG_STAY_DISCOUNT));
        BigDecimal total = subtotal.subtract(discount);
        return new PricingResult(nights, subtotal, discount, total);
    }

    private BigDecimal calculateNightlyTotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, currentDate);
            total = total.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        return round(total);
    }

    private BigDecimal calculateNightPrice(Room room, LocalDate date) {
        BigDecimal baseRate = room.getNightlyRate();

        if (isHighSeason(date)) {
            baseRate = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }

        return round(baseRate);
    }

    private boolean isHighSeason(LocalDate date) {
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

    public BigDecimal calculateRefund(RatePlan ratePlan, BigDecimal amountPaid) {
        return switch (ratePlan) {
            case STANDARD, CORPORATE, LONG_STAY -> amountPaid;
            case NON_REFUNDABLE -> BigDecimal.ZERO;
            case WEEKEND_PACKAGE -> round(amountPaid.multiply(WEEKEND_REFUND_RATE));
        };
    }

    public void validateRatePlan(RatePlan ratePlan, LocalDate checkIn, LocalDate checkOut,
                                String companyCode) {
        int nights = (int) checkIn.until(checkOut).getDays();

        switch (ratePlan) {
            case CORPORATE:
                if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
                    throw new IllegalArgumentException("Invalid company code for corporate rate plan");
                }
                break;
            case WEEKEND_PACKAGE:
                if (nights != 2 || checkIn.getDayOfWeek() != DayOfWeek.FRIDAY ||
                    checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    throw new IllegalArgumentException("Weekend package requires Friday check-in and Sunday check-out");
                }
                break;
            case LONG_STAY:
                if (nights < 7) {
                    throw new IllegalArgumentException("Long stay requires minimum 7 nights");
                }
                break;
        }
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
