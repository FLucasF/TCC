package com.pousadamareaalta.pricing;

import com.pousadamareaalta.exception.InvalidRequestException;
import com.pousadamareaalta.model.RoomType;

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

    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
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
    public PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(roomType);
        return new PricingResult(packagePrice, BigDecimal.ZERO, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return SeasonUtil.round(amountPaid.multiply(REFUND_RATE));
    }
}
