package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<String, BigDecimal> PACKAGE_PRICES = Map.of(
        "STANDARD", new BigDecimal("520.00"),
        "DELUXE", new BigDecimal("820.00"),
        "SUITE", new BigDecimal("1200.00")
    );

    @Override
    public String getName() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validateAppliesTo(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlan.InvalidRatePlanException {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RatePlan.InvalidRatePlanException("Check-in must be on Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new RatePlan.InvalidRatePlanException("Check-out must be on Sunday");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (days != 2) {
            throw new RatePlan.InvalidRatePlanException("Weekend package must be exactly 2 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal price = PACKAGE_PRICES.get(room.type);
        if (price == null) {
            throw new IllegalArgumentException("Unknown room type: " + room.type);
        }
        return new PriceCalculation(price, BigDecimal.ZERO, price);
    }

    @Override
    public BigDecimal getRefundAmount(BigDecimal amountPaid) {
        return amountPaid.multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
