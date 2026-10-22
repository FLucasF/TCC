package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights != 2) {
            throw new InvalidRequestException("Weekend package requires exactly 2 nights");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new InvalidRequestException("Weekend package must start on Friday");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new InvalidRequestException("Weekend package must end on Sunday");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PACKAGE_PRICES.get(room.getType());
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid.divide(new BigDecimal("2"), 2, RoundingMode.HALF_EVEN);
    }
}
