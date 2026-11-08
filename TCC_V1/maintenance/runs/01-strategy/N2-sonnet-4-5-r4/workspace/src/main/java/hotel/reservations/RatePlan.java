package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate with company-specific discounts; full refund on cancellation. */
    CORPORATE,
    /** Weekend package with fixed prices for Friday to Sunday stays; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay discount for 7+ nights; full refund on cancellation. */
    LONG_STAY
}
