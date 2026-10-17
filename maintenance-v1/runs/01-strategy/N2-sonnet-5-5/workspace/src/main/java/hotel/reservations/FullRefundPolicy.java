package hotel.reservations;

import java.math.BigDecimal;

/** Base for plans that give back everything that was paid. */
abstract class FullRefundPolicy implements RatePlanPolicy {

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
