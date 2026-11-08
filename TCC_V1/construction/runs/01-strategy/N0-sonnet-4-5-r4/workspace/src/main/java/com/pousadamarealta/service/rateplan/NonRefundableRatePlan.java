package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.PricingService;

import java.math.BigDecimal;

public class NonRefundableRatePlan implements RatePlanStrategy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private final StandardRatePlan standardPlan;
    private final PricingService pricingService;

    public NonRefundableRatePlan(PricingService pricingService) {
        this.pricingService = pricingService;
        this.standardPlan = new StandardRatePlan(pricingService);
    }

    @Override
    public void validate(Reservation reservation, Room room) {
    }

    @Override
    public BigDecimal calculateSubtotal(Reservation reservation, Room room) {
        return standardPlan.calculateSubtotal(reservation, room);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, Reservation reservation) {
        return pricingService.round(subtotal.multiply(DISCOUNT_RATE));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return BigDecimal.ZERO;
    }
}
