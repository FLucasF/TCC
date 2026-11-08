package com.pousada.domain.rateplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends AbstractRatePlan {

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
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = checkIn;

        while (currentNight.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(nightlyRate, currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }

        return roundMoney(subtotal);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, String companyCode) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = subtotal.multiply(discountRate);
        return roundMoney(discount);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
