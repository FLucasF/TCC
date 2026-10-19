package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

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
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());
        return new PricingResult(packagePrice, MoneyUtils.round(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return MoneyUtils.round(amountPaid.divide(new BigDecimal("2")));
    }
}
