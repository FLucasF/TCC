package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.PricingService;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

public class LongStayRatePlan implements RatePlanStrategy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    private final StandardRatePlan standardPlan;
    private final PricingService pricingService;

    public LongStayRatePlan(PricingService pricingService) {
        this.pricingService = pricingService;
        this.standardPlan = new StandardRatePlan(pricingService);
    }

    @Override
    public void validate(Reservation reservation, Room room) {
        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new ApiException(400, "INVALID_REQUEST", "Long stay requires at least " + MINIMUM_NIGHTS + " nights");
        }
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
        return reservation.getAmountPaid();
    }
}
