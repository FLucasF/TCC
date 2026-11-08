package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public String getName() {
        return "LONG_STAY";
    }

    @Override
    public void validateAppliesTo(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlan.InvalidRatePlanException {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < MIN_NIGHTS) {
            throw new RatePlan.InvalidRatePlanException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal nightlyRate = SeasonalHelper.applySeasonalRate(room.nightlyRate, nightDate);
            nightlyRate = SeasonalHelper.roundHalfEven(nightlyRate);
            subtotal = subtotal.add(nightlyRate);
        }

        subtotal = SeasonalHelper.roundHalfEven(subtotal);
        BigDecimal discount = subtotal.multiply(DISCOUNT_RATE);
        discount = SeasonalHelper.roundHalfEven(discount);
        BigDecimal total = subtotal.subtract(discount);
        total = SeasonalHelper.roundHalfEven(total);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal getRefundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
