package com.pousada.rateplan;

import com.pousada.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Map;

public class CorporateRatePlan extends BaseRatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(PricingContext context) {
        if (context.getCompanyCode() == null || context.getCompanyCode().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST",
                "Company code is required for corporate rate plan", 400);
        }

        if (!COMPANY_DISCOUNTS.containsKey(context.getCompanyCode())) {
            throw new BusinessException("INVALID_REQUEST",
                "Invalid company code: " + context.getCompanyCode(), 400);
        }
    }

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal subtotal = calculateNightlySubtotal(context);
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(context.getCompanyCode());
        BigDecimal discount = round(subtotal.multiply(discountRate));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
