package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.PricingService;

import java.math.BigDecimal;
import java.util.Map;

public class CorporateRatePlan implements RatePlanStrategy {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    private final StandardRatePlan standardPlan;
    private final PricingService pricingService;

    public CorporateRatePlan(PricingService pricingService) {
        this.pricingService = pricingService;
        this.standardPlan = new StandardRatePlan(pricingService);
    }

    @Override
    public void validate(Reservation reservation, Room room) {
        String companyCode = reservation.getCompanyCode();
        if (companyCode == null || companyCode.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid company code: " + companyCode);
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Reservation reservation, Room room) {
        return standardPlan.calculateSubtotal(reservation, room);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, Reservation reservation) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(reservation.getCompanyCode());
        return pricingService.round(subtotal.multiply(discountRate));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
