package com.pousadamareaita.pricing;

import com.pousadamareaita.model.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackagePlanStrategy implements RatePlanStrategy {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, BigDecimal.valueOf(520.00),
        RoomType.DELUXE, BigDecimal.valueOf(820.00),
        RoomType.SUITE, BigDecimal.valueOf(1200.00)
    );

    private final RoomType roomType;

    public WeekendPackagePlanStrategy(RoomType roomType) {
        this.roomType = roomType;
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int guests, int roomCapacity, String companyCode) {
        if (!checkIn.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            throw new IllegalArgumentException("Check-in must be on Friday");
        }
        if (!checkOut.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            throw new IllegalArgumentException("Check-out must be on Sunday");
        }
        int nights = PricingUtils.countNights(checkIn, checkOut);
        if (nights != 2) {
            throw new IllegalArgumentException("Weekend package must be exactly 2 nights");
        }
        if (guests < 1) {
            throw new IllegalArgumentException("Must have at least 1 guest");
        }
        if (guests > roomCapacity) {
            throw new IllegalArgumentException("Guests exceed room capacity");
        }
    }

    @Override
    public PriceCalculation calculatePrice(LocalDate checkIn, LocalDate checkOut, BigDecimal baseNightlyRate, int guests) {
        int nights = 2;
        BigDecimal packagePrice = PACKAGE_PRICES.get(roomType);
        BigDecimal subtotal = packagePrice;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = packagePrice;

        return new PriceCalculation(nights, subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PricingUtils.roundBanker(amountPaid.multiply(BigDecimal.valueOf(0.5)));
    }
}
