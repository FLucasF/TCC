package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.pousadamarealta.domain.service.rateplan.SeasonCalculator.*;

public class CorporateRatePlan implements RatePlan {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Código de empresa inválido para plano corporativo");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = applyHighSeasonIfNeeded(roomType.getNightlyRate(), current);
            total = total.add(nightRate);
            current = current.plusDays(1);
        }

        return roundHalfToEven(total);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, String companyCode) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        return roundHalfToEven(subtotal.multiply(discountRate));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
