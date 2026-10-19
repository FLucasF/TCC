package com.pousadamarealta.domain;

public class RatePlanFactory {
    public static RatePlan create(RatePlanType type) {
        return switch (type) {
            case STANDARD -> new StandardRatePlan();
            case NON_REFUNDABLE -> new NonRefundableRatePlan();
        };
    }
}
