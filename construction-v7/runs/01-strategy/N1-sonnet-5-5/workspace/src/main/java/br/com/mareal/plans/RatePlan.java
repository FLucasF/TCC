package br.com.mareal.plans;

import java.math.BigDecimal;

/**
 * A rate plan: how the stay is priced, who may use it and what is returned on cancellation.
 * To add a plan, implement this interface and register it in {@link RatePlans}.
 */
public interface RatePlan {
    String code();

    /** Prices the stay; throws an INVALID_REQUEST ApiException if the plan cannot be used for it. */
    Pricing price(Stay stay);

    /** Amount returned to the guest when cancelling a reservation on which {@code amountPaid} was paid. */
    BigDecimal refund(BigDecimal amountPaid);
}
