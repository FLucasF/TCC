package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    private String companyCode;

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || companyCode.isEmpty()) {
            throw new InvalidRequestException("Company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Invalid company code");
        }
        this.companyCode = companyCode;
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        return subtotal.multiply(discountRate)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
