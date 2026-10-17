package hotel.reservations;

import java.math.BigDecimal;

/** The plans the inn sells. Each one knows how it prices, who can book it, and what it gives back. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** The discount the guest's company agreed with the inn; everything comes back. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday at a closed price; half comes back. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** Seven nights or more with 10% off; everything comes back. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    /** Refuses, with INVALID_REQUEST, a stay that does not fit this plan's rules. */
    public void checkEligibility(Stay stay) {
        policy.checkEligibility(stay);
    }

    public Price price(Stay stay) {
        return policy.price(stay);
    }

    /** What the guest gets back, out of what was paid, when cancelling before the check-in. */
    public BigDecimal refund(BigDecimal amountPaid) {
        return policy.refund(amountPaid);
    }
}
