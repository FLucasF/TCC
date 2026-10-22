package com.pousada.rateplan;

import com.pousada.domain.Room;
import com.pousada.utils.Money;
import com.pousada.utils.Season;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", BigDecimal.valueOf(12),
            "AGROVALE", BigDecimal.valueOf(18),
            "FARMAVIDA", BigDecimal.valueOf(25)
    );

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException {
        if (companyCode == null || companyCode.isBlank()) {
            throw new RatePlanException("INVALID_REQUEST", "Company code is required for corporate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode.toUpperCase())) {
            throw new RatePlanException("INVALID_REQUEST", "Invalid company code");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            BigDecimal multiplier = Season.getSeasonMultiplier(date);
            subtotal = Money.add(subtotal, Money.multiply(nightlyRate, multiplier));
        }

        return new PriceCalculation(subtotal, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }

    public BigDecimal getCompanyDiscount(String companyCode) {
        return COMPANY_DISCOUNTS.get(companyCode.toUpperCase());
    }
}
