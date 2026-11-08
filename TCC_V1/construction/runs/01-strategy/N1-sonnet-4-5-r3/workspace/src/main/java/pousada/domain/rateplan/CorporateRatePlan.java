package pousada.domain.rateplan;

import pousada.domain.RoomType;

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
    public String name() {
        return "CORPORATE";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Company code is required and must be valid for CORPORATE rate plan");
        }
    }

    @Override
    public PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        BigDecimal subtotal = calculateSubtotalForNights(checkIn, checkOut, roomType);
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
