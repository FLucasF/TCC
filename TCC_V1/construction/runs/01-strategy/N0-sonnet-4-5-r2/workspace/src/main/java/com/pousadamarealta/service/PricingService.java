package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

public class PricingService {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );
    private static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    public void calculatePricing(Reservation reservation, Room room) {
        int nights = (int) reservation.getCheckIn().datesUntil(reservation.getCheckOut()).count();
        reservation.setNights(nights);

        switch (reservation.getRatePlan()) {
            case WEEKEND_PACKAGE -> calculateWeekendPackage(reservation, room);
            case CORPORATE -> calculateCorporate(reservation, room);
            case LONG_STAY -> calculateLongStay(reservation, room);
            case NON_REFUNDABLE -> calculateNonRefundable(reservation, room);
            default -> calculateStandard(reservation, room);
        }
    }

    private void calculateStandard(Reservation reservation, Room room) {
        BigDecimal subtotal = calculateSubtotal(reservation, room.getNightlyRate());
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(subtotal);
    }

    private void calculateNonRefundable(Reservation reservation, Room room) {
        BigDecimal subtotal = calculateSubtotal(reservation, room.getNightlyRate());
        reservation.setSubtotal(subtotal);

        BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"));
        discount = roundHalfToEven(discount);
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        reservation.setTotal(total);
    }

    private void calculateCorporate(Reservation reservation, Room room) {
        String companyCode = reservation.getCompanyCode();
        if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Invalid company code for corporate rate plan");
        }

        BigDecimal subtotal = calculateSubtotal(reservation, room.getNightlyRate());
        reservation.setSubtotal(subtotal);

        BigDecimal discountRate = CORPORATE_DISCOUNTS.get(companyCode);
        BigDecimal discount = subtotal.multiply(discountRate);
        discount = roundHalfToEven(discount);
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        reservation.setTotal(total);
    }

    private void calculateWeekendPackage(Reservation reservation, Room room) {
        if (reservation.getNights() != 2) {
            throw new IllegalArgumentException("Weekend package requires exactly 2 nights");
        }

        LocalDate checkIn = reservation.getCheckIn();
        LocalDate checkOut = reservation.getCheckOut();

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Weekend package must be Friday to Sunday");
        }

        BigDecimal packagePrice = WEEKEND_PACKAGE_PRICES.get(room.getType());
        reservation.setSubtotal(packagePrice);
        reservation.setDiscount(BigDecimal.ZERO);
        reservation.setTotal(packagePrice);
    }

    private void calculateLongStay(Reservation reservation, Room room) {
        if (reservation.getNights() < 7) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }

        BigDecimal subtotal = calculateSubtotal(reservation, room.getNightlyRate());
        reservation.setSubtotal(subtotal);

        BigDecimal discount = subtotal.multiply(new BigDecimal("0.10"));
        discount = roundHalfToEven(discount);
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        reservation.setTotal(total);
    }

    private BigDecimal calculateSubtotal(Reservation reservation, BigDecimal baseRate) {
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate date = reservation.getCheckIn();
        while (date.isBefore(reservation.getCheckOut())) {
            BigDecimal nightRate = baseRate;
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(HIGH_SEASON_MULTIPLIER);
                nightRate = roundHalfToEven(nightRate);
            }
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }

        return roundHalfToEven(subtotal);
    }

    private boolean isHighSeason(LocalDate date) {
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

    public BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal refundAmount = switch (reservation.getRatePlan()) {
            case NON_REFUNDABLE -> BigDecimal.ZERO;
            case WEEKEND_PACKAGE -> {
                BigDecimal half = reservation.getAmountPaid().divide(new BigDecimal("2"), 10, RoundingMode.HALF_EVEN);
                yield roundHalfToEven(half);
            }
            default -> reservation.getAmountPaid();
        };

        return refundAmount;
    }

    private BigDecimal roundHalfToEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
