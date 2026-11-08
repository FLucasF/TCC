package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public String getName() {
        return "CORPORATE";
    }

    @Override
    public void validateAppliesTo(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlan.InvalidRatePlanException {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new RatePlan.InvalidRatePlanException("Invalid or missing company code");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        throw new UnsupportedOperationException("Use calculatePrice(room, checkIn, checkOut, companyCode)");
    }

    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal nightlyRate = SeasonalHelper.applySeasonalRate(room.nightlyRate, nightDate);
            nightlyRate = SeasonalHelper.roundHalfEven(nightlyRate);
            subtotal = subtotal.add(nightlyRate);
        }

        subtotal = SeasonalHelper.roundHalfEven(subtotal);

        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = subtotal.multiply(discountRate);
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
