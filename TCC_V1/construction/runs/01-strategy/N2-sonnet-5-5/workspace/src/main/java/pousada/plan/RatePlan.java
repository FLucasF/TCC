package pousada.plan;

import pousada.Quote;
import pousada.Stay;

import java.math.BigDecimal;

/** One way of pricing a reservation: who may use it, what it costs and what is refunded on cancellation. */
public interface RatePlan {
    String code();

    /** Throws INVALID_REQUEST when the stay cannot use this plan. */
    void validate(Stay stay);

    Quote quote(Stay stay);

    BigDecimal refund(BigDecimal amountPaid);
}
