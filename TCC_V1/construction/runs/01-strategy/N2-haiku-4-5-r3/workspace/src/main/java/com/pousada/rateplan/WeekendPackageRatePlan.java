package com.pousada.rateplan;

import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<String, BigDecimal> PACKAGE_PRICES = Map.of(
            "STANDARD", new BigDecimal("520.00"),
            "DELUXE", new BigDecimal("820.00"),
            "SUITE", new BigDecimal("1200.00")
    );

    @Override
    public void validate(int roomCapacity, int nights, LocalDate checkIn, LocalDate checkOut,
                         int guests, Map<String, Object> context) throws RatePlanException {
        if (nights != 2) {
            throw new RatePlanException("Weekend package requires exactly 2 nights");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RatePlanException("Weekend package requires check-in on Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new RatePlanException("Weekend package requires check-out on Sunday");
        }
    }

    @Override
    public PriceCalculation calculate(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut,
                                      Map<String, Object> context) {
        String roomType = (String) context.get("roomType");
        BigDecimal price = PACKAGE_PRICES.get(roomType);

        BigDecimal subtotal = price;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = price;

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return Money.round(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
