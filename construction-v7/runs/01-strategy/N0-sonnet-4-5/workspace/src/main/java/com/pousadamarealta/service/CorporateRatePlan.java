package com.pousadamarealta.service;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Código de empresa inválido para o plano corporativo");
        }
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = calculateNightRate(room.getType(), date);
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }

        subtotal = round(subtotal);
        return new PricingResult(subtotal, BigDecimal.ZERO, subtotal);
    }

    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = calculateNightRate(room.getType(), date);
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }

        subtotal = round(subtotal);
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = round(subtotal.multiply(discountRate));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
