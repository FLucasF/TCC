package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends AbstractRatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("12"),
        "AGROVALE", new BigDecimal("18"),
        "FARMAVIDA", new BigDecimal("25")
    );

    private String companyCode;

    @Override
    public String getName() {
        return "CORPORATE";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new InvalidRequestException("Company code is required for corporate rate plan");
        }

        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Invalid company code: " + companyCode);
        }

        this.companyCode = companyCode;
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return calculateStandardSubtotal(checkIn, checkOut, roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        return MoneyUtil.percentage(subtotal, discountRate);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
