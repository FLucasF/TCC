package com.pousadamareaalta.pricing;

import com.pousadamareaalta.exception.InvalidRequestException;
import com.pousadamareaalta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends StandardRatePlan {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new InvalidRequestException("Company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Invalid company code");
        }
    }

    @Override
    public PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(roomType, checkIn, nights);
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = SeasonUtil.round(subtotal.multiply(discountRate));
        BigDecimal total = SeasonUtil.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
