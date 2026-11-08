package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlanStrategy {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights != 2) {
            throw new IllegalArgumentException("Weekend package requires exactly 2 nights");
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new IllegalArgumentException("Weekend package must start on Friday");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Weekend package must end on Sunday");
        }
    }

    @Override
    public PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(roomType);
        return new PriceCalculation(packagePrice, BigDecimal.ZERO, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return SeasonCalculator.round(amountPaid.divide(new BigDecimal("2"), 10, java.math.RoundingMode.HALF_EVEN));
    }
}
