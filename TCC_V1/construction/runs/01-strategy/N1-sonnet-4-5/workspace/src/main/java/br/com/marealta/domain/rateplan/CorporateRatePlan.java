package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", BigDecimal.valueOf(12),
        "AGROVALE", BigDecimal.valueOf(18),
        "FARMAVIDA", BigDecimal.valueOf(25)
    );

    @Override
    public void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new IllegalArgumentException("Company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode.toUpperCase())) {
            throw new IllegalArgumentException("Invalid company code: " + companyCode);
        }
    }

    @Override
    public Money calculateSubtotal(RoomType roomType, DateRange dateRange) {
        return dateRange.calculateNightlyTotal(roomType.getNightlyRate());
    }

    @Override
    public Money calculateDiscount(Money subtotal) {
        return Money.zero();
    }

    public Money calculateDiscount(Money subtotal, String companyCode) {
        BigDecimal discountPercentage = COMPANY_DISCOUNTS.get(companyCode.toUpperCase());
        return subtotal.discount(discountPercentage);
    }

    @Override
    public Money calculateRefund(Money amountPaid) {
        return amountPaid;
    }

    @Override
    public String getType() {
        return "CORPORATE";
    }
}
