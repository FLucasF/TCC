package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardPlan implements RatePlan {
    @Override
    public String getName() {
        return "STANDARD";
    }

    @Override
    public void validateAppliesTo(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        // Standard plan applies to all dates and guests
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
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal getRefundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
